package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.model.TimeoutException;
import com.github.hermannpencole.nifi.config.utils.FunctionUtils;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.ConnectionsApi;
import com.github.hermannpencole.nifi.swagger.client.FlowfileQueuesApi;
import com.github.hermannpencole.nifi.swagger.client.model.ConnectionEntity;
import com.github.hermannpencole.nifi.swagger.client.model.DropRequestEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Class that offer service for process group
 * <p>
 * Created by SFRJ on 01/04/2017.
 */
@Singleton
public class ConnectionService {


    /**
     * The logger.
     */
    private final static Logger LOG = LoggerFactory.getLogger(ConnectionService.class);

    @Named("timeout")
    @Inject
    public Integer timeout;

    @Named("interval")
    @Inject
    public Integer interval;

    @Named("forceMode")
    @Inject
    public Boolean forceMode;

    @Inject
    private ConnectionsApi connectionsApi;

    @Inject
    private FlowfileQueuesApi flowfileQueuesApi;

    public void waitEmptyQueue(ConnectionEntity connectionEntity) throws ApiException {
        try {
            FunctionUtils.runWhile(() -> {
                ConnectionEntity connection = connectionsApi.getConnection(connectionEntity.getId());
                LOG.info(" {} : there is {} FlowFile ({} bytes) on the queue ", connection.getId(), connection.getStatus().getAggregateSnapshot().getQueuedCount(), connection.getStatus().getAggregateSnapshot().getQueuedSize());
                return !connection.getStatus().getAggregateSnapshot().getQueuedCount().equals("0");
            }, interval, timeout);
        } catch (TimeoutException e) {
            //empty queue if forced mode
            if (forceMode) {
                DropRequestEntity dropRequest= flowfileQueuesApi.createDropRequest(connectionEntity.getId());
                FunctionUtils.runWhile(() -> {
                    DropRequestEntity drop = flowfileQueuesApi.getDropRequest(connectionEntity.getId(), dropRequest.getDropRequest().getId());
                    return !drop.getDropRequest().getFinished();
                }, interval, timeout);
                LOG.info(" {} : {} FlowFile ({} bytes) were removed from the queue", connectionEntity.getId(), dropRequest.getDropRequest().getCurrentCount(), dropRequest.getDropRequest().getCurrentSize());
                flowfileQueuesApi.removeDropRequest(connectionEntity.getId(), dropRequest.getDropRequest().getId());
            } else {
                LOG.error(e.getMessage(),e);
                throw e;
            }
        }

    }

}
