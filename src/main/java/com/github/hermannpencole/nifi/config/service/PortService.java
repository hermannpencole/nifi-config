package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.model.ConfigException;
import com.github.hermannpencole.nifi.config.utils.FunctionUtils;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.InputPortsApi;
import com.github.hermannpencole.nifi.swagger.client.OutputPortsApi;
import com.github.hermannpencole.nifi.swagger.client.ProcessGroupsApi;
import com.github.hermannpencole.nifi.swagger.client.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Class that offer service for process group
 * <p>
 * Created by SFRJ on 01/04/2017.
 */
@Singleton
public class PortService {

    /**
     * The logger.
     */
    private final static Logger LOG = LoggerFactory.getLogger(PortService.class);

    @Named("timeout")
    @Inject
    public Integer timeout;

    @Named("interval")
    @Inject
    public Integer interval;

    @Inject
    private InputPortsApi inputPortsApi;

    @Inject
    private OutputPortsApi outputPortsApi;

    @Inject
    private ProcessGroupsApi processGroupsApi;


    /**
     * the the state of port
     *
     * @param port
     * @param state
     */
    public void setState(PortEntity port, PortDTO.StateEnum state) {
        if (port.getComponent().getState().equals(PortDTO.StateEnum.DISABLED)) {
            LOG.info(" {} ({}) is disabled ", port.getComponent().getName(), port.getId());
            return;
        }
        if (port.getStatus().getRunStatus().equals("Invalid")) {
            LOG.info(" {} ({}) is invalid ", port.getComponent().getName(), port.getId());
            return;
        }
        //how obtain state of and don't have this bullshit trick
        //trick for don't have error : xxxx cannot be started because it is not stopped. Current state is STOPPING
        if (port.getComponent().getState().equals(state)) {
            LOG.info(" {} ({}) is already {}", port.getComponent().getName() ,port.getId(), port.getComponent().getState());
            return;
        }

        FunctionUtils.runWhile(()-> {
            boolean haveResult = false;
            try {
                PortEntity body = new PortEntity();
                body.setRevision(port.getRevision());
                body.setComponent(new PortDTO());
                body.getComponent().setState(state);
                body.getComponent().setId(port.getId());
                body.setBulletins(null);
                body.getComponent().setTransmitting(null);
                body.getComponent().setUserAccessControl(null);
                body.getComponent().setGroupAccessControl(null);
                body.getComponent().setValidationErrors(null);
                PortEntity portEntity;
                if (port.getComponent().getType() == PortDTO.TypeEnum.INPUT_PORT)
                    portEntity = inputPortsApi.updateInputPort(port.getId(), body);
                else
                    portEntity = outputPortsApi.updateOutputPort(port.getId(), body);
                LOG.info(" {} ({}) is {} ", portEntity.getComponent().getName(), portEntity.getId(), portEntity.getComponent().getState());
                haveResult = true;
            } catch (ApiException e) {
                if (e.getResponseBody() == null || !e.getResponseBody().endsWith("Current state is STOPPING")) {
                    logErrors(port);
                    throw new ConfigException(e.getMessage() + ": " + e.getResponseBody(), e);
                }
                LOG.info(e.getResponseBody());
            }
            return !haveResult;
        }, interval, timeout);

    }

    private Optional<PortEntity> findPortEntityByName(final Stream<PortEntity> portEntities, final String name) {
        return portEntities.filter(item -> item.getComponent().getName().trim().equals(name.trim())).findFirst();
    }

    public Optional<PortEntity> findPortEntityByName(final FlowDTO flow, final String componentName) {
        Optional<PortEntity> result = findPortEntityByName(flow.getOutputPorts().stream(), componentName);
        if (!result.isPresent()) {
            result = findPortEntityByName(flow.getInputPorts().stream(), componentName);
        }
        return result;
    }


    public Predicate<ConnectableDTO> isPort() {
        return c -> c.getType() == ConnectableDTO.TypeEnum.OUTPUT_PORT
                || c.getType() == ConnectableDTO.TypeEnum.INPUT_PORT;
    }

    public PortEntity getById(String id, PortDTO.TypeEnum type) {
        if (type == PortDTO.TypeEnum.INPUT_PORT)
            return inputPortsApi.getInputPort(id);
        else
            return outputPortsApi.getOutputPort(id);
    }

    /**
     * Creates an input or output port.
     *
     * @param processGroupId GUID of the process group in which to create the port
     * @param name           The name of the port
     * @param type           Whether to create an input or an output port
     * @return A data transfer object representative of the state of the created port
     */
    public PortEntity createPort(final String processGroupId, final String name, final PortDTO.TypeEnum type) {
        PortEntity portEntity = new PortEntity();
        portEntity.setRevision(new RevisionDTO());
        portEntity.setComponent(new PortDTO());
        portEntity.getRevision().setVersion(0L);
        portEntity.getComponent().setName(name);
        switch (type) {
            case INPUT_PORT:
                return processGroupsApi.createInputPort(processGroupId, portEntity);
            case OUTPUT_PORT: default:
                return processGroupsApi.createOutputPort(processGroupId, portEntity);
        }
    }



    /**
     * log the error reported by port
     *
     * @param port
     */
    private void logErrors(PortEntity port) {
        try {
            PortEntity portEntity;
            if (port.getComponent().getType()== PortDTO.TypeEnum.INPUT_PORT)
                portEntity = inputPortsApi.getInputPort(port.getId());
            else
                portEntity = outputPortsApi.getOutputPort(port.getId());
            portEntity.getComponent().getValidationErrors().stream().forEach(msg -> LOG.error(msg));
        } catch (ApiException e1) {
            LOG.error(e1.getMessage());
        }
    }

}
