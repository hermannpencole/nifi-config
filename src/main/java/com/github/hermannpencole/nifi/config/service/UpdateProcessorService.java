package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.model.ConfigException;
import com.github.hermannpencole.nifi.config.model.GroupProcessorsEntity;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.FlowApi;
import com.github.hermannpencole.nifi.swagger.client.ProcessorsApi;
import com.github.hermannpencole.nifi.swagger.client.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.hermannpencole.nifi.config.utils.FunctionUtils.findByComponentName;

/**
 * Class that offer service for nifi processor
 * <p>
 * Created by SFRJ on 01/04/2017.
 */
@Singleton
public class UpdateProcessorService {

    /**
     * The logger.
     */
    private final static Logger LOG = LoggerFactory.getLogger(UpdateProcessorService.class);

    @Inject
    private ProcessGroupService processGroupService;

    @Inject
    private ControllerServicesService controllerServicesService;

    @Inject
    private CreateRouteService createRouteService;

    @Inject
    private FlowApi flowapi;

    @Inject
    private ProcessorsApi processorsApi;

    /**
     * @param branch
     * @param fileConfiguration
     * @throws IOException
     * @throws URISyntaxException
     * @throws ApiException
     */
    public void updateByBranch(List<String> branch, String fileConfiguration, boolean optionNoStartProcessors) throws IOException, ApiException {
        File file = new File(fileConfiguration);
        if (!file.exists()) {
            throw new FileNotFoundException("Repository " + file.getName() + " is empty or doesn't exist");
        }

        LOG.info("Processing : " + file.getName());
        Gson gson = new GsonBuilder().serializeNulls().create();

        try (Reader reader = new InputStreamReader(new FileInputStream(file), "UTF-8")) {
            GroupProcessorsEntity configuration = gson.fromJson(reader, GroupProcessorsEntity.class);
            ProcessGroupFlowEntity componentSearch = processGroupService.changeDirectory(branch)
                    .orElseThrow(() -> new ConfigException(("cannot find " + Arrays.toString(branch.toArray()))));

            //Stop branch
            processGroupService.stop(componentSearch);
            LOG.info(Arrays.toString(branch.toArray()) + " is stopped");

            //Stop connexion ??

            //the state change, then the revision also in nifi 1.3.0 (only?) reload processGroup
            componentSearch = flowapi.getFlow(componentSearch.getProcessGroupFlow().getId());

            //generate clientID
            String clientId = flowapi.generateClientId();
            updateComponent(configuration, componentSearch, clientId);

            //controller
            updateControllers(configuration, componentSearch.getProcessGroupFlow().getId(), clientId);

            //connexion
            createRouteService.createRoutes(configuration.getConnectionPorts(), optionNoStartProcessors);

            if (!optionNoStartProcessors) {
                //Run all nifi processors
                componentSearch = flowapi.getFlow(componentSearch.getProcessGroupFlow().getId());
                processGroupService.start(componentSearch);
                //setState(componentSearch, ProcessorDTO.StateEnum.RUNNING);
                LOG.info(Arrays.toString(branch.toArray()) + " is running");
            }
        } catch (InterruptedException e) {
            LOG.error(e.getMessage(), e);
        } finally {
            LOG.debug("updateByBranch end");
        }
    }


    /**
     *
     * @param configuration
     * @param idComponent
     * @throws ApiException
     */
    private void updateControllers(GroupProcessorsEntity configuration, String idComponent, String clientId) throws ApiException, InterruptedException {
        ControllerServicesEntity controllerServicesEntity = flowapi.getControllerServicesFromGroup(idComponent);
        //must we use flowapi.getControllerServicesFromController() ??
        /*ControllerServicesEntity controllerServiceController = flowapi.getControllerServicesFromController();
        for (ControllerServiceEntity controllerServiceEntity: controllerServiceController.getControllerServices()) {
            controllerServicesEntity.addControllerServicesItem(controllerServiceEntity);
        }*/

        for (ControllerServiceDTO controllerServiceDTO : configuration.getControllerServicesDTO()) {
            List<ControllerServiceEntity> all = controllerServicesEntity.getControllerServices().stream().filter(item -> item.getComponent().getName().trim().equals(controllerServiceDTO.getName().trim())).collect(Collectors.toList());

            ControllerServiceEntity controllerServiceEntityFind = null;
            Map<String, ControllerServiceEntity> oldControllersService = new HashMap<>();
            if (all.size() > 1) {
                for (ControllerServiceEntity controllerServiceEntity: all) {
                    if (idComponent.equals(controllerServiceEntity.getComponent().getParentGroupId())) {
                        //add to old
                        oldControllersService.put(controllerServiceEntity.getId(), controllerServiceEntity);
                    } else {
                        controllerServiceEntityFind = controllerServiceEntity;
                    }
                }
            } else if (all.size() == 1) {
                //find controller for have id
                controllerServiceEntityFind = all.get(0);
            } else {
                throw new ConfigException(("cannot find " + controllerServiceDTO.getName()));
            }

            //remove old
            removeOldReference(oldControllersService.values());

            //stopping referencing processors and reporting tasks
            //  controllerServicesService.setStateReferenceProcessors(controllerServiceEntityFind, UpdateControllerServiceReferenceRequestEntity.StateEnum.STOPPED);

            //Disabling referencing controller services
            controllerServicesService.setStateReferencingControllerServices(controllerServiceEntityFind.getId(), UpdateControllerServiceReferenceRequestEntity.StateEnum.DISABLED);

            //update new reference for ReferencingComponents on oldControllersService
            updateOldReference(oldControllersService.values(), controllerServiceEntityFind.getId(), clientId);

            //Enabling this controller service
            ControllerServiceEntity controllerServiceEntityUpdate = controllerServicesService.updateControllerService(controllerServiceDTO, controllerServiceEntityFind);

            //Enabling referencing controller services
            controllerServicesService.setStateReferencingControllerServices(controllerServiceEntityFind.getId(), UpdateControllerServiceReferenceRequestEntity.StateEnum.ENABLED);

            //Starting referencing processors and reporting tasks
          //  controllerServicesService.setStateReferenceProcessors(controllerServiceEntityUpdate, UpdateControllerServiceReferenceRequestEntity.StateEnum.RUNNING);
        }
    }

    /**
     * Update controller to newControllerServiceId for ReferencingComponents on oldControllersService
     *
     * @param newControllerServiceId
     * @param oldControllersService
     */
    private void updateOldReference(Collection<ControllerServiceEntity> oldControllersService, String newControllerServiceId, String clientId) {
        for (ControllerServiceEntity oldControllerService: oldControllersService) {
            for (ControllerServiceReferencingComponentEntity component : oldControllerService.getComponent().getReferencingComponents()) {
                ProcessorEntity newProc = processorsApi.getProcessor(component.getId());
                updateProperties(newProc, oldControllerService.getId(), newControllerServiceId);
                updateProcessor(newProc, newProc.getComponent(), true, clientId);
                //processorsApi.updateProcessor(newProc.getId(), newProc);
            }
        }
    }

    private void removeOldReference(Collection<ControllerServiceEntity> oldControllersService) {
        for (ControllerServiceEntity oldControllerService: oldControllersService) {
            try {
                //maybe there are already remove
                controllerServicesService.getControllerServices(oldControllerService.getId());
                //Disabling referencing controller services
                controllerServicesService.setStateReferencingControllerServices(oldControllerService.getId(), UpdateControllerServiceReferenceRequestEntity.StateEnum.DISABLED);
                //remove
                controllerServicesService.remove(oldControllerService);
            } catch (ApiException e) {
                if (e.getMessage().contains("Not Found")){

                } else throw e;
            }
        }
    }

    private void updateProperties(ProcessorEntity proc, String oldValue, String newValue) {
        for (Map.Entry<String, String> entry : proc.getComponent().getConfig().getProperties().entrySet()) {
            if (oldValue.equals(entry.getValue())) {
                proc.getComponent().getConfig().getProperties().put(entry.getKey(), newValue);
            }
        }
    }

    /**
     * @param configuration
     * @param componentSearch
     * @param clientId
     * @throws ApiException
     */
    private void updateComponent(GroupProcessorsEntity configuration, ProcessGroupFlowEntity componentSearch, String clientId) throws ApiException {
        FlowDTO flow = componentSearch.getProcessGroupFlow().getFlow();
        configuration.getProcessors().forEach(processorOnConfig -> updateProcessor(
                findProcByComponentName(flow.getProcessors(), processorOnConfig.getName()), processorOnConfig, false, clientId));
        for (GroupProcessorsEntity procGroupInConf : configuration.getGroupProcessorsEntity()) {
            ProcessGroupEntity processorGroupToUpdate = findByComponentName(flow.getProcessGroups(), procGroupInConf.getName())
                    .orElseThrow(() -> new ConfigException(("cannot find " + procGroupInConf.getName())));
            updateComponent(procGroupInConf, flowapi.getFlow(processorGroupToUpdate.getId()), clientId);
        }
    }

    /**
     * update processor configuration with valueToPutInProc
     * at first find id of each processor and in second way update it
     *
     * @param processorToUpdate
     * @param componentToPutInProc
     * @param clientId
     */
    private void updateProcessor(ProcessorEntity processorToUpdate, ProcessorDTO componentToPutInProc, boolean forceController, String clientId) {
        try {
            componentToPutInProc.setId(processorToUpdate.getId());
            LOG.info("Update processor : " + processorToUpdate.getComponent().getName());
            //update on nifi
            List<String> autoTerminatedRelationships = new ArrayList<>();
            processorToUpdate.getComponent().getRelationships().stream()
                    .filter(relationships -> relationships.getAutoTerminate())
                    .forEach(relationships -> autoTerminatedRelationships.add(relationships.getName()));
            componentToPutInProc.getConfig().setAutoTerminatedRelationships(autoTerminatedRelationships);
            componentToPutInProc.getConfig().setDescriptors(processorToUpdate.getComponent().getConfig().getDescriptors());
            componentToPutInProc.getConfig().setDefaultConcurrentTasks(processorToUpdate.getComponent().getConfig().getDefaultConcurrentTasks());
            componentToPutInProc.getConfig().setDefaultSchedulingPeriod(processorToUpdate.getComponent().getConfig().getDefaultSchedulingPeriod());
            componentToPutInProc.setRelationships(processorToUpdate.getComponent().getRelationships());
            componentToPutInProc.setStyle(processorToUpdate.getComponent().getStyle());
            componentToPutInProc.setSupportsBatching(processorToUpdate.getComponent().getSupportsBatching());
            componentToPutInProc.setSupportsEventDriven(processorToUpdate.getComponent().getSupportsEventDriven());
            componentToPutInProc.setSupportsParallelProcessing(processorToUpdate.getComponent().getSupportsParallelProcessing());
            componentToPutInProc.setPersistsState(processorToUpdate.getComponent().getPersistsState());
            componentToPutInProc.setRestricted(null);//processorToUpdate.getComponent().getRestricted());
            componentToPutInProc.setValidationErrors(processorToUpdate.getComponent().getValidationErrors());
            //remove controller link
            if (! forceController) {
                for (Map.Entry<String, PropertyDescriptorDTO> entry : processorToUpdate.getComponent().getConfig().getDescriptors().entrySet()) {
                    if (entry.getValue().getIdentifiesControllerService() != null) {
                        componentToPutInProc.getConfig().getProperties().remove(entry.getKey());
                    }
                }
            }
            processorToUpdate.setComponent(componentToPutInProc);
            processorToUpdate.getRevision().setClientId(clientId);

            processorsApi.updateProcessor(processorToUpdate.getId(), processorToUpdate);

            //nifiService.updateProcessorProperties(toUpdate, componentToPutInProc.getString("id"));
            LOG.info("Updated : " + componentToPutInProc.getName());
        } catch (ApiException e) {
            throw new ConfigException(e.getMessage() + ": " + e.getResponseBody(), e);
        }
    }

    //can static => utils
    public static ProcessorEntity findProcByComponentName(List<ProcessorEntity> listGroup, String name) {
        return listGroup.stream()
                .filter(item -> item.getComponent().getName().trim().equals(name.trim()))
                .findFirst().orElseThrow(() -> new ConfigException(("cannot find " + name)));
    }

}
