package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.model.TimeoutException;
import com.github.hermannpencole.nifi.config.utils.FunctionUtils;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.ConnectionsApi;
import com.github.hermannpencole.nifi.swagger.client.FlowApi;
import com.github.hermannpencole.nifi.swagger.client.FlowfileQueuesApi;
import com.github.hermannpencole.nifi.swagger.client.ProcessGroupsApi;
import com.github.hermannpencole.nifi.swagger.client.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;

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

    @Inject
    private FlowApi flowApi;

    @Inject
    private ProcessGroupsApi processGroupsApi;

    @Inject
    private ProcessorService processorService;

    @Inject
    private PortService portService;

    private boolean stopProcessorOrPort(String id) {
        ProcessorEntity processorEntity = null;
        try {
            processorEntity = processorService.getById(id);
        } catch (ApiException e) {
            //do nothing
        }
        if (processorEntity != null) {
            processorService.setState(processorEntity, ProcessorDTO.StateEnum.STOPPED);
            return true;
        }

        PortEntity portEntity = null;
        try {
            portEntity = portService.getById(id, PortDTO.TypeEnum.INPUT_PORT);
        } catch (ApiException e) {
            try {
                portEntity = portService.getById(id, PortDTO.TypeEnum.OUTPUT_PORT);
            } catch (ApiException e2) {
                LOG.info("Couldn't find processor or port to stop for id ({}).", id);
                return false;
            }
        }
        if (portEntity != null) {
            portService.setState(portEntity, PortDTO.StateEnum.STOPPED);
        }

        return false;
    }

    public boolean isEmptyQueue(ConnectionEntity connectionEntity) throws ApiException {
        return connectionsApi.getConnection(connectionEntity.getId()).getStatus().getAggregateSnapshot().getQueuedCount().equals("0");
    }

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
                DropRequestEntity dropRequest = flowfileQueuesApi.createDropRequest(connectionEntity.getId());
                FunctionUtils.runWhile(() -> {
                    DropRequestEntity drop = flowfileQueuesApi.getDropRequest(connectionEntity.getId(), dropRequest.getDropRequest().getId());
                    return !drop.getDropRequest().isFinished();
                }, interval, timeout);
                LOG.info(" {} : {} FlowFile ({} bytes) were removed from the queue", connectionEntity.getId(), dropRequest.getDropRequest().getCurrentCount(), dropRequest.getDropRequest().getCurrentSize());
                flowfileQueuesApi.removeDropRequest(connectionEntity.getId(), dropRequest.getDropRequest().getId());
            } else {
                LOG.error(e.getMessage(), e);
                throw e;
            }
        }
    }

    public void removeExternalConnections(ProcessGroupEntity processGroupEntity) {
        final String groupId = processGroupEntity.getComponent().getId();

        ProcessGroupFlowEntity flow = flowApi.getFlow(groupId);
        final List<ConnectionEntity> groupConnections = processGroupsApi.getConnections(
                flow.getProcessGroupFlow().getParentGroupId()).getConnections();

        groupConnections.forEach(connection -> {
            if (connection.getDestinationGroupId().equals(groupId) || connection.getSourceGroupId().equals(groupId)) {
                //stopping source/destination
                if (connection.getDestinationGroupId().equals(groupId)) {
                    stopProcessorOrPort(connection.getSourceId());
                }
                if (connection.getSourceGroupId().equals(groupId)) {
                    stopProcessorOrPort(connection.getDestinationId());
                }

                connectionsApi.deleteConnection(
                        connection.getComponent().getId(),
                        connection.getRevision().getVersion().toString(),
                        flowApi.generateClientId());
            }
        });
    }

}
