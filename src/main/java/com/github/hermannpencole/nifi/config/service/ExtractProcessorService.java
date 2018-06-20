package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.model.ConfigException;
import com.github.hermannpencole.nifi.config.model.Connection;
import com.github.hermannpencole.nifi.config.model.GroupProcessorsEntity;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.FlowApi;
import com.github.hermannpencole.nifi.swagger.client.model.*;
import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that offer service for nifi processor
 * <p>
 * Created by SFRJ on 01/04/2017.
 */
@Singleton
public class ExtractProcessorService {


    /**
     * The logger.
     */
    private final static Logger LOG = LoggerFactory.getLogger(ExtractProcessorService.class);

    @Inject
    private ProcessGroupService processGroupService;

    @Inject
    private FlowApi flowapi;

    /**
     * @param branch
     * @param fileConfiguration
     * @throws IOException
     * @throws ApiException
     */
    public void extractByBranch(List<String> branch, String fileConfiguration, boolean failOnDuplicateNames) throws IOException, ApiException {
        File file = new File(fileConfiguration);

        ProcessGroupFlowEntity componentSearch = processGroupService.changeDirectory(branch)
                .orElseThrow(() -> new ConfigException(("cannot find " + Arrays.toString(branch.toArray()))));

        //add group processors and processors
        GroupProcessorsEntity result = extractJsonFromComponent(componentSearch);

        //add controllers
        String processorGroupFlowId = componentSearch.getProcessGroupFlow().getId();
        ControllerServicesEntity controllerServicesEntity = flowapi.getControllerServicesFromGroup(processorGroupFlowId);
        if (!controllerServicesEntity.getControllerServices().isEmpty()) {
            result.setControllerServicesDTO(new ArrayList<>());
        }
        for (ControllerServiceEntity controllerServiceEntity : controllerServicesEntity.getControllerServices()) {
            result.getControllerServicesDTO().add(extractController(controllerServiceEntity));
        }

        checkDuplicateProcessorNames(result.getProcessors(), failOnDuplicateNames);

        //convert to json
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        LOG.debug("saving in file {}", fileConfiguration);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
            gson.toJson(result, writer);
        } finally {
            LOG.debug("extractByBranch end");
        }
    }

    private void checkDuplicateProcessorNames(List<ProcessorDTO> processors, boolean failOnDuplicateNames) {
        //warn or fail on duplicate processor names
        if (processors == null || processors.isEmpty()) {
            return;
        }

        Map<String, Integer> duplicateProcessorNames = detectDuplicateProcessorNames(processors);
        if (!duplicateProcessorNames.isEmpty()) {
            String messageFormatted = "Duplicate processor names detected: "
                    + Joiner.on(", ").withKeyValueSeparator(" used times: ").join(duplicateProcessorNames);

            if (failOnDuplicateNames) {
                throw new ConfigException(messageFormatted);
            } else {
                LOG.warn(messageFormatted);
            }
        }
    }

    private Map<String, Integer> detectDuplicateProcessorNames(List<ProcessorDTO> processorList) {
        Map<String, Integer> processorNameCountMap = new HashMap<>();
        processorList.forEach(proc -> processorNameCountMap.merge(proc.getName(), 1, Integer::sum));

        return processorNameCountMap.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * extract from component
     *
     * @param idComponent
     * @return
     * @throws ApiException
     */
    private GroupProcessorsEntity extractJsonFromComponent(ProcessGroupFlowEntity idComponent) throws ApiException {
        GroupProcessorsEntity result = new GroupProcessorsEntity();
        ProcessGroupFlowDTO processGroupFlow = idComponent.getProcessGroupFlow();
        result.setName(processGroupFlow.getBreadcrumb().getBreadcrumb().getName());
        processGroupFlow.getFlow().getProcessors()
                .forEach(processor -> result.getProcessors().add(extractProcessor(processor.getComponent())));
        for (ProcessGroupEntity processGroups : processGroupFlow.getFlow().getProcessGroups()) {
            result.getGroupProcessorsEntity().add(extractJsonFromComponent(flowapi.getFlow(processGroups.getId())));
        }
        if (result.getGroupProcessorsEntity().isEmpty()) {
            result.setGroupProcessorsEntity(null);
        }
        if (result.getProcessors().isEmpty()) {
            result.setProcessors(null);
        }
        result.setControllerServicesDTO(null);

        List<ConnectionEntity> connections = idComponent.getProcessGroupFlow().getFlow().getConnections();
        result.setConnections(extractConnections(connections));

        return result;
    }

    /**
     * extract processor configuration
     *
     * @param processor
     * @return
     */
    private ProcessorDTO extractProcessor(ProcessorDTO processor) {
        ProcessorDTO result = new ProcessorDTO();
        result.setName(processor.getName());
        result.setConfig(processor.getConfig());
        //remove controller link
        for (Map.Entry<String, PropertyDescriptorDTO> entry : processor.getConfig().getDescriptors().entrySet()) {
            if (entry.getValue().getIdentifiesControllerService() != null) {
                result.getConfig().getProperties().remove(entry.getKey());
            }
        }
        result.getConfig().setAutoTerminatedRelationships(null);
        result.getConfig().setDescriptors(null);
        result.getConfig().setDefaultConcurrentTasks(null);
        result.getConfig().setDefaultSchedulingPeriod(null);
        result.setRelationships(null);
        result.setStyle(null);
        result.setSupportsBatching(null);
        result.setSupportsEventDriven(null);
        result.setSupportsParallelProcessing(null);
        result.setPersistsState(null);
        result.setRestricted(null);
        result.setValidationErrors(null);

        return result;
    }

    private ControllerServiceDTO extractController(ControllerServiceEntity controllerServiceEntity) {
        ControllerServiceDTO result = new ControllerServiceDTO();
        result.setName(controllerServiceEntity.getComponent().getName());
        result.setProperties(controllerServiceEntity.getComponent().getProperties());
        result.setPersistsState(null);
        result.setRestricted(null);
        result.setDescriptors(null);
        result.setReferencingComponents(null);
        result.setValidationErrors(null);
        return result;
    }

    private List<Connection> extractConnections(List<ConnectionEntity> connections) {
        return connections
                .stream()
                .map(this::toConnection)
                .collect(Collectors.toList());
    }

    private Connection toConnection(ConnectionEntity entity) {
        ConnectionDTO dto = entity.getComponent();
        Connection connection = new Connection();
        connection.setId(entity.getId());
        connection.setDestination(dto.getDestination().getName());
        connection.setSource(dto.getSource().getName());
        connection.setName(dto.getName());
        connection.setBackPressureDataSizeThreshold(dto.getBackPressureDataSizeThreshold());
        connection.setBackPressureObjectThreshold(dto.getBackPressureObjectThreshold());
        return connection;
    }
}
