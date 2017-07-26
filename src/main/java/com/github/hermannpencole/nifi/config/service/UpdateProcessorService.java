package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.model.ConfigException;
import com.github.hermannpencole.nifi.config.model.GroupProcessorsEntity;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.FlowApi;
import com.github.hermannpencole.nifi.swagger.client.ProcessorsApi;
import com.github.hermannpencole.nifi.swagger.client.model.*;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    public void updateByBranch(List<String> branch, String fileConfiguration) throws IOException, ApiException {
        File file = new File(fileConfiguration);
        if (!file.exists()) {
            throw new FileNotFoundException("Repository " + file.getName() + " is empty or doesn't exist");
        }

        LOG.info("Processing : " + file.getName());
        Gson gson = new Gson();
        try (Reader reader = new InputStreamReader(new FileInputStream(file), "UTF-8")) {
            GroupProcessorsEntity configuration = gson.fromJson(reader, GroupProcessorsEntity.class);
            ProcessGroupFlowEntity componentSearch = processGroupService.changeDirectory(branch)
                    .orElseThrow(() -> new ConfigException(("cannot find " + Arrays.toString(branch.toArray()))));

            //Stop branch
            processGroupService.setState(componentSearch.getProcessGroupFlow().getId(), ScheduleComponentsEntity.StateEnum.STOPPED);
            LOG.info(Arrays.toString(branch.toArray()) + " is stopped");

            //the state change, then the revision also in nifi 1.3.0 (only?) reload processGroup
            componentSearch = flowapi.getFlow(componentSearch.getProcessGroupFlow().getId());

            //generate clientID
            String clientId = flowapi.generateClientId();
            updateComponent(configuration, componentSearch, clientId);

            //Run all nifi processors
            processGroupService.setState(componentSearch.getProcessGroupFlow().getId(), ScheduleComponentsEntity.StateEnum.RUNNING);
            LOG.info(Arrays.toString(branch.toArray()) + " is running");
        } finally {
            LOG.debug("updateByBranch end");
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
        configuration.getProcessors().forEach(processorOnConfig -> updateProcessor(flow.getProcessors(), processorOnConfig, clientId));
        for (GroupProcessorsEntity procGroupInConf : configuration.getGroupProcessorsEntity()) {
            ProcessGroupEntity processorGroupToUpdate = ProcessGroupService.findByComponentName(flow.getProcessGroups(), procGroupInConf.getName())
                    .orElseThrow(() -> new ConfigException(("cannot find " + procGroupInConf.getName())));
            updateComponent(procGroupInConf, flowapi.getFlow(processorGroupToUpdate.getId()), clientId);
        }
    }

    /**
     * update processor configuration with valueToPutInProc
     * at first find id of each processor and in second way update it
     *  @param processorsList
     * @param componentToPutInProc
     * @param clientId
     */
    private void updateProcessor(List<ProcessorEntity> processorsList, ProcessorDTO componentToPutInProc, String clientId) {
        try {
            ProcessorEntity processorToUpdate = findProcByComponentName(processorsList, componentToPutInProc.getName());
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
            componentToPutInProc.setRestricted(processorToUpdate.getComponent().getRestricted());
            componentToPutInProc.setValidationErrors(processorToUpdate.getComponent().getValidationErrors());
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
