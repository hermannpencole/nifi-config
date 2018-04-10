package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.model.Connection;
import com.github.hermannpencole.nifi.swagger.client.model.*;

/**
 * Created by SFRJ2737 on 2017-05-29.
 */
public class TestUtils {

    public static ProcessGroupFlowEntity createProcessGroupFlowEntity(String id, String name) {
        ProcessGroupFlowDTO componentSearch = new ProcessGroupFlowDTO();
        componentSearch.setId(id);
        componentSearch.setBreadcrumb(new FlowBreadcrumbEntity());
        componentSearch.getBreadcrumb().setBreadcrumb(new FlowBreadcrumbDTO());
        componentSearch.getBreadcrumb().getBreadcrumb().setName(name);

        FlowDTO flow = new FlowDTO();
        ProcessGroupFlowEntity response = new ProcessGroupFlowEntity();
        response.setProcessGroupFlow(componentSearch);
        response.getProcessGroupFlow().setFlow(flow);
        return response;
    }

    public static ProcessGroupEntity createProcessGroupEntity(String id, String name) {
        ProcessGroupEntity processGroupEntity = new ProcessGroupEntity();
        processGroupEntity.setId(id);
        ProcessGroupDTO processGroupDTO = new ProcessGroupDTO();
        processGroupDTO.setName(name);
        processGroupDTO.setId(id);
        processGroupEntity.setComponent(processGroupDTO);
        processGroupEntity.setRevision(createRevision(10L));
        processGroupEntity.setPosition(new PositionDTO());
        processGroupEntity.getPosition().setX(0d);
        processGroupEntity.getPosition().setY(0d);
        return processGroupEntity;
    }

    public static RevisionDTO createRevision(Long version) {
        RevisionDTO revision = new RevisionDTO();
        revision.setVersion(version);
        return revision;
    }

    public static ProcessorEntity createProcessorEntity(String id, String name) {
        ProcessorEntity proc = new ProcessorEntity();
        proc.setId(id);
        ProcessorDTO procDTO = new ProcessorDTO();
        procDTO.setName(name);
        procDTO.setId(id);
        procDTO.setConfig(new ProcessorConfigDTO());
        proc.setComponent(procDTO);
        proc.setRevision(new RevisionDTO());
        proc.getRevision().setVersion(100L);
        proc.setPosition(new PositionDTO());
        proc.getPosition().setX(0d);
        proc.getPosition().setY(0d);
        return proc;
    }

    public static ConnectionEntity createConnectionEntity(String name, String sourceName, String destinationName,
                                                          String backPressureDataSizeThreshold,
                                                          Long backPressureObjectThreshold) {
        ConnectionEntity connectionEntity = new ConnectionEntity();
        connectionEntity.setId(name + "id");

        ConnectionDTO connectionDTO = new ConnectionDTO();
        connectionDTO.setName(name);
        connectionEntity.setComponent(connectionDTO);

        ConnectableDTO source = new ConnectableDTO();
        source.setName(sourceName);
        connectionDTO.setSource(source);

        ConnectableDTO destination = new ConnectableDTO();
        destination.setName(destinationName);
        connectionDTO.setDestination(destination);

        connectionDTO.setBackPressureDataSizeThreshold(backPressureDataSizeThreshold);
        connectionDTO.setBackPressureObjectThreshold(backPressureObjectThreshold);
        return connectionEntity;
    }

    public static ConnectionEntity createConnectionEntity(String id, String sourceId, String destinationId) {
        ConnectionEntity connectionEntity = new ConnectionEntity();
        connectionEntity.setId(id);
        connectionEntity.setDestinationId(destinationId);
        connectionEntity.setSourceId(sourceId);
        connectionEntity.setRevision(createRevision(10L));
        return connectionEntity;
    }

    public static Connection createConnection(String name, String source, String destination, String dataSizeThreashold, Long objectThreshold) {
        Connection connection = new Connection();
        connection.setName(name);
        connection.setSource(source);
        connection.setDestination(destination);
        connection.setBackPressureDataSizeThreshold(dataSizeThreashold);
        connection.setBackPressureObjectThreshold(objectThreshold);
        return connection;
    }

    public static ControllerServiceEntity createControllerServiceEntity(String id, String name) {
        ControllerServiceEntity controllerService = new ControllerServiceEntity();
        controllerService.setId(id);
        controllerService.setComponent(new ControllerServiceDTO());
        controllerService.getComponent().setName(name);
        controllerService.getComponent().setId(id);
        controllerService.getComponent().getProperties().put("key", "value");
        controllerService.setRevision(createRevision(10L));
        return controllerService;
    }
}
