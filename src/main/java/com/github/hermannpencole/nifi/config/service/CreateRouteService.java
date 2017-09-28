package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.model.ConfigException;
import com.github.hermannpencole.nifi.config.model.RouteConnectionEntity;
import com.github.hermannpencole.nifi.config.model.RouteConnectionsEntity;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.FlowApi;
import com.github.hermannpencole.nifi.swagger.client.InputPortsApi;
import com.github.hermannpencole.nifi.swagger.client.OutputPortsApi;
import com.github.hermannpencole.nifi.swagger.client.ProcessGroupsApi;
import com.github.hermannpencole.nifi.swagger.client.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.github.hermannpencole.nifi.config.utils.FunctionUtils.findByComponentName;

public final class CreateRouteService {

    /**
     * The logger.
     */
    private final static Logger LOG = LoggerFactory.getLogger(CreateRouteService.class);

    private String getClientId() {
        if (clientId == null) {
            clientId = flowapi.generateClientId();
        }
        return clientId;
    }

    private String clientId;

    @Inject
    private FlowApi flowapi;

    @Inject
    private InputPortsApi inputPortsApi;

    @Inject
    private OutputPortsApi outputPortsApi;

    @Inject
    private ProcessGroupsApi processGroupsApi;

    private Optional<PortEntity> findPortEntityByName(final Stream<PortEntity> portEntities, final String name) {
        return portEntities.filter(item -> item.getComponent().getName().trim().equals(name.trim())).findFirst();
    }

    private PortDTO.TypeEnum matchConnectableTypeToPortType(final ConnectableDTO.TypeEnum connectableType) {
        switch (connectableType) {
            case INPUT_PORT:
                return PortDTO.TypeEnum.INPUT_PORT;
            case OUTPUT_PORT:
                return PortDTO.TypeEnum.OUTPUT_PORT;
            default:
                throw new ConfigException("Cannot convert " + connectableType + " to PortDTO");
        }
    }

    private ConnectableDTO.TypeEnum matchPortTypeToConnectableType(final PortDTO.TypeEnum portType) {
        switch (portType) {
            case INPUT_PORT:
                return ConnectableDTO.TypeEnum.INPUT_PORT;
            case OUTPUT_PORT:
                return ConnectableDTO.TypeEnum.OUTPUT_PORT;
            default:
                throw new ConfigException("Cannot convert " + portType + " to ConnectableDTO");
        }
    }

    /**
     * Creates an input or output port.
     *
     * @param processGroupId GUID of the process group in which to create the port
     * @param name           The name of the port
     * @param type           Whether to create an input or an output port
     * @return A data transfer object representative of the state of the created port
     */
    private PortEntity createPort(final String processGroupId, final String name, final PortDTO.TypeEnum type) {
        PortEntity portEntity = new PortEntity();
        portEntity.setRevision(new RevisionDTO());
        portEntity.setComponent(new PortDTO());
        portEntity.getRevision().setVersion(0L);
        portEntity.getRevision().setClientId(getClientId());
        portEntity.getComponent().setName(name);
        switch (type) {
            case INPUT_PORT:
                return processGroupsApi.createInputPort(processGroupId, portEntity);
            case OUTPUT_PORT:
                return processGroupsApi.createOutputPort(processGroupId, portEntity);
        }
        throw new ConfigException(String.format("Couldn't create port '{}'", name));
    }

    private ConnectableDTO createConnectableDTOFromPort(final PortEntity port) {
        ConnectableDTO connectableDTO = new ConnectableDTO();
        connectableDTO.setGroupId(port.getComponent().getParentGroupId());
        connectableDTO.setName(port.getComponent().getName());
        connectableDTO.setId(port.getComponent().getId());
        connectableDTO.setType(matchPortTypeToConnectableType(port.getComponent().getType()));

        return connectableDTO;
    }

    private ConnectableDTO findConnectableComponent(
            final FlowDTO flow,
            final String componentName) {
        return findPortEntityByName(flow.getOutputPorts().stream(), componentName)
                .map(this::createConnectableDTOFromPort)
                .orElse(
                        findPortEntityByName(flow.getInputPorts().stream(), componentName)
                                .map(this::createConnectableDTOFromPort)
                                .orElse(null));
    }

    private ProcessGroupFlowEntity advanceToNextProcessGroup(
            final String processGroupName,
            final ProcessGroupFlowEntity flowEntity) {
        return findByComponentName(
                flowEntity.getProcessGroupFlow().getFlow().getProcessGroups(), processGroupName)
                .map(flowEntityChild -> flowapi.getFlow(flowEntityChild.getId()))
                .orElseThrow(() -> new ConfigException("Couldn't find process group '" + processGroupName + "'"));
    }

    private ConnectableDTO createOrFindPort(
            final String destinationInputPort,
            final ConnectableDTO.TypeEnum connectableType,
            final ProcessGroupFlowEntity flowEntity,
            final String processGroupName) {
        ProcessGroupFlowDTO processGroupFlow = flowEntity.getProcessGroupFlow();
        ConnectableDTO connectableDTO = findConnectableComponent(processGroupFlow.getFlow(), destinationInputPort);
        if (connectableDTO != null && connectableDTO.getType() == connectableType) {
            return connectableDTO;
        }
        if (connectableDTO == null) {
            return createConnectableDTOFromPort(createPort(
                    processGroupFlow.getId(),
                    destinationInputPort,
                    matchConnectableTypeToPortType(connectableType)));
        }
        throw new ConfigException("'" + destinationInputPort + "' in '" + processGroupName
                + "' is not a " + connectableType);
    }

    private String determineConnectionLocation(
            final ConnectableDTO source,
            final ConnectableDTO destination) {
        switch (source.getType()) {
            case OUTPUT_PORT:
                switch (destination.getType()) {
                    case OUTPUT_PORT:
                        return destination.getGroupId();
                    case INPUT_PORT:
                        return flowapi.getFlow(source.getGroupId()).getProcessGroupFlow().getParentGroupId();
                }
            case INPUT_PORT:
                switch (destination.getType()) {
                    case OUTPUT_PORT:
                        return source.getGroupId();
                    case INPUT_PORT:
                        return source.getGroupId();
                }
        }

        throw new ConfigException("Creating connections between types other than local ports not supported");
    }

    private ConnectionEntity createConnectionEntity(
            final ConnectableDTO source,
            final ConnectableDTO dest) {
        ConnectionEntity connectionEntity = new ConnectionEntity();
        connectionEntity.setRevision(new RevisionDTO());
        connectionEntity.getRevision().setVersion(0L);
        connectionEntity.getRevision().setClientId(getClientId());
        connectionEntity.setComponent(new ConnectionDTO());
        connectionEntity.getComponent().setSource(source);
        connectionEntity.getComponent().setDestination(dest);

        return connectionEntity;
    }

    private boolean connectionExists(
            final Stream<ConnectionEntity> connectionEntities,
            final ConnectionEntity connectionEntity) {
        return connectionEntities.anyMatch(item ->
                item.getComponent().getSource().getGroupId().equals(
                        connectionEntity.getComponent().getSource().getGroupId())
                        && item.getComponent().getSource().getId().equals(
                        connectionEntity.getComponent().getSource().getId())
                        && item.getComponent().getSource().getName().trim().equals(
                        connectionEntity.getComponent().getSource().getName().trim())
                        && item.getComponent().getSource().getType().equals(
                        connectionEntity.getComponent().getSource().getType())
                        && item.getComponent().getDestination().getGroupId().equals(
                        connectionEntity.getComponent().getDestination().getGroupId())
                        && item.getComponent().getDestination().getId().equals(
                        connectionEntity.getComponent().getDestination().getId())
                        && item.getComponent().getDestination().getName().trim().equals(
                        connectionEntity.getComponent().getDestination().getName().trim())
                        && item.getComponent().getDestination().getType().equals(
                        connectionEntity.getComponent().getDestination().getType())
        );
    }

    private void startPort(String id, PortDTO.TypeEnum type) {
        PortEntity portEntity;

        switch (type) {
            case OUTPUT_PORT:
                portEntity = outputPortsApi.getOutputPort(id);
                break;
            case INPUT_PORT:
                portEntity = inputPortsApi.getInputPort(id);
                break;
            default:
                throw new ConfigException("Unknown port type '" + type + "'");
        }

        if (portEntity.getComponent().getState() == PortDTO.StateEnum.RUNNING) return;

        portEntity.getComponent().setState(PortDTO.StateEnum.RUNNING);
        portEntity.getStatus().setRunStatus("Running");
        portEntity.getRevision().setClientId(getClientId());

        switch (type) {
            case OUTPUT_PORT:
                outputPortsApi.updateOutputPort(id, portEntity);
                return;
            case INPUT_PORT:
                inputPortsApi.updateInputPort(id, portEntity);
                return;
            default:
                throw new ConfigException("Unknown port type '" + type + "'");
        }
    }

    private Predicate<ConnectableDTO> isPort() {
        return c -> c.getType() == ConnectableDTO.TypeEnum.OUTPUT_PORT
                || c.getType() == ConnectableDTO.TypeEnum.INPUT_PORT;
    }

    private List<ConnectableDTO> createPorts(
            final ListIterator<String> branch,
            final String destinationInputPort,
            final ConnectableDTO.TypeEnum connectableType)
            throws ApiException {
        List<ConnectableDTO> connectableDTOs = new ArrayList<>();
        int mergeLevel = branch.nextIndex();
        boolean createPorts = false;

        // Traverse back up to root and start examining the flow
        while (branch.hasPrevious()) branch.previous();
        ProcessGroupFlowEntity flowEntity = flowapi.getFlow("root");
        branch.next();

        // Traverse back down the process group hierarchy
        // Create output ports from the position in the hierarchy where the branch iterator initially pointed
        while (branch.hasNext()) {
            if (branch.nextIndex() == mergeLevel) {
                createPorts = true;
            }
            String processGroupName = branch.next();
            flowEntity = advanceToNextProcessGroup(processGroupName, flowEntity);
            if (createPorts) {
                connectableDTOs.add(createOrFindPort(
                        destinationInputPort,
                        connectableType,
                        flowEntity,
                        processGroupName));
            }
        }

        return connectableDTOs;
    }

    private void createConnections(final ListIterator<ConnectableDTO> connectables) {
        ConnectableDTO current;
        ConnectableDTO next = connectables.next();
        while (connectables.hasNext()) {
            current = next;
            next = connectables.next();

            ProcessGroupFlowEntity flowEntity = flowapi.getFlow(determineConnectionLocation(current, next));

            ConnectionEntity connectionEntity = createConnectionEntity(current, next);

            if (!connectionExists(
                    flowEntity.getProcessGroupFlow().getFlow().getConnections().stream(),
                    connectionEntity)) {
                processGroupsApi.createConnection(flowEntity.getProcessGroupFlow().getId(), connectionEntity);
            }
        }
    }

    private void startPorts(Stream<ConnectableDTO> connectables) {
        connectables.filter(isPort()).forEach(
                port -> startPort(port.getId(), matchConnectableTypeToPortType(port.getType())));
    }

    /**
     * Create a route in NiFi composed of ports and connections.
     *
     * @param name            Name of the route
     * @param sourcePath      Path to the process group from which to create the route
     * @param destinationPath Path to the process group to which to create the route
     * @param startRoute      Whether to put ports along the route into a running state
     */
    private void createRoute(
            final String name,
            final List<String> sourcePath,
            final List<String> destinationPath,
            final boolean startRoute) {
        ListIterator<String> source = sourcePath.listIterator();
        ListIterator<String> destination = destinationPath.listIterator();

        // Find the lowest level in the process group hierarchy where the route can pass between two process groups
        while (!source.next().equals(destination.next())) ;

        // Traverse the source branch, creating output ports down the hierarchy
        List<ConnectableDTO> sourceConnectables
                = createPorts(source, name, ConnectableDTO.TypeEnum.OUTPUT_PORT);
        // Reverse the sequence of the output ports as the connections should point up the hierarchy
        Collections.reverse(sourceConnectables);

        // Traverse the destination branch, creating input ports up the hierarchy
        List<ConnectableDTO> destinationConnectables
                = createPorts(destination, name, ConnectableDTO.TypeEnum.INPUT_PORT);

        // Stitch the two lists of connectables together
        List<ConnectableDTO> route = new ArrayList<>(sourceConnectables);
        route.addAll(destinationConnectables);
        createConnections(route.listIterator());

        // Switch on ports
        if (startRoute) {
            startPorts(route.stream());
        }
    }

    /**
     * Create routes described by configuration.
     *
     * @param fileConfiguration       Configuration file describing connections
     * @param optionNoStartProcessors Whether or not to start ports created along the route
     * @throws IOException If repository file cannot be found
     */
    public void createRoutes(String fileConfiguration, boolean optionNoStartProcessors) throws IOException {
        File file = new File(fileConfiguration);
        if (!file.exists()) {
            throw new FileNotFoundException("Repository " + file.getName() + " is empty or doesn't exist");
        }

        LOG.info("Processing : " + file.getName());
        Gson gson = new GsonBuilder().serializeNulls().create();
        try (Reader reader = new InputStreamReader(new FileInputStream(file), "UTF-8")) {
            RouteConnectionsEntity connections = gson.fromJson(reader, RouteConnectionsEntity.class);

            for (RouteConnectionEntity routeConnectionEntity : connections.getConnections()) {
                createRoute(
                        routeConnectionEntity.getName(),
                        routeConnectionEntity.getSourceList(),
                        routeConnectionEntity.getDestinationList(),
                        !optionNoStartProcessors);
            }
        }
    }
}
