package com.github.hermannpencole.nifi.config.service;

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
        RevisionDTO revision = new RevisionDTO();
        revision.setVersion(10L);
        processGroupEntity.setRevision(revision);
        return processGroupEntity;
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
        return proc;
    }

    public static ControllerServicesEntity createControllerServicesEntity(String id, String name) {
        ControllerServicesEntity controllerServicesEntity = new ControllerServicesEntity();
        controllerServicesEntity.getControllerServices().add(new ControllerServiceEntity());
        controllerServicesEntity.getControllerServices().get(0).setId(id);
        controllerServicesEntity.getControllerServices().get(0).setComponent(new ControllerServiceDTO());
        controllerServicesEntity.getControllerServices().get(0).getComponent().setName(name);
        controllerServicesEntity.getControllerServices().get(0).getComponent().setId(id);
        controllerServicesEntity.getControllerServices().get(0).getComponent().getProperties().put("key", "value");
        return controllerServicesEntity;
    }
}
