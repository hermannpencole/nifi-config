package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.model.ConfigException;
import com.github.hermannpencole.nifi.config.utils.FunctionUtils;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.ProcessorsApi;
import com.github.hermannpencole.nifi.swagger.client.model.ProcessorDTO;
import com.github.hermannpencole.nifi.swagger.client.model.ProcessorEntity;
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
public class ProcessorService {

    /**
     * The logger.
     */
    private final static Logger LOG = LoggerFactory.getLogger(ProcessorService.class);

    @Named("timeout")
    @Inject
    public Integer timeout;

    @Named("interval")
    @Inject
    public Integer interval;

    @Inject
    private ProcessorsApi processorsApi;

    /**
     * the the state of processor
     *
     * @param processor
     * @param state
     */
    public void setState(ProcessorEntity processor, ProcessorDTO.StateEnum state) {
        //how obtain state of and don't have this bullshit trick
        //trick for don't have error : xxxx cannot be started because it is not stopped. Current state is STOPPING
        if (processor.getComponent().getState().equals(state)) {
            LOG.info(" {} ({}) is already ", processor.getComponent().getName() ,processor.getId(), processor.getComponent().getState());
            return;
        }

        FunctionUtils.runWhile(()-> {
            boolean haveResult = false;
            try {
                ProcessorEntity body = new ProcessorEntity();
                body.setRevision(processor.getRevision());
                body.setComponent(new ProcessorDTO());
                body.getComponent().setState(state);
                body.getComponent().setId(processor.getId());
                body.getComponent().setRestricted(null);
                ProcessorEntity processorEntity= processorsApi.updateProcessor(processor.getId(), body);
                LOG.info(" {} ({}) is {} ", processorEntity.getComponent().getName(), processorEntity.getId(), processorEntity.getComponent().getState());
                haveResult = true;
            } catch (ApiException e) {
                if (e.getResponseBody() == null || !e.getResponseBody().endsWith("Current state is STOPPING")) {
                    logErrors(processor);
                    throw new ConfigException(e.getMessage() + ": " + e.getResponseBody(), e);
                }
                LOG.info(e.getResponseBody());
            }
            return !haveResult;
        }, interval, timeout);

    }

    /**
     * log the error reported by processor
     *
     * @param processor
     */
    private void logErrors(ProcessorEntity processor) {
        try {
            ProcessorEntity procInError = processorsApi.getProcessor(processor.getId());
            procInError.getComponent().getValidationErrors().stream().forEach(msg -> LOG.error(msg));
        } catch (ApiException e1) {
            LOG.error(e1.getMessage());
        }
    }

}
