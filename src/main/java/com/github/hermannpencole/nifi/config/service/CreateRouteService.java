package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.model.ConfigException;
import com.github.hermannpencole.nifi.config.utils.FunctionUtils;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.FlowApi;
import com.github.hermannpencole.nifi.swagger.client.ProcessGroupsApi;
import com.github.hermannpencole.nifi.swagger.client.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Stream;

public class CreateRouteService {

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
    private ProcessGroupsApi processGroupsApi;

    private Optional<PortEntity> findPortEntityByName(Stream<PortEntity> portEntities, String name) {
        return portEntities.filter(item -> item.getComponent().getName().trim().equals(name.trim())).findFirst();
    }

    private PortDTO.TypeEnum matchConnectableTypeToPortType(ConnectableDTO.TypeEnum connectableType) {
        switch (connectableType) {
            case INPUT_PORT:
                return PortDTO.TypeEnum.INPUT_PORT;
            case OUTPUT_PORT:
                return PortDTO.TypeEnum.OUTPUT_PORT;
            default:
                throw new ConfigException("Cannot convert " + connectableType + " to PortDTO");
        }
    }

    private ConnectableDTO.TypeEnum matchPortTypeToConnectableType(PortDTO.TypeEnum portType) {
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
    private PortEntity createPort(String processGroupId, String name, PortDTO.TypeEnum type) {
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

    private ConnectableDTO createConnectableDTOFromPort(PortEntity port) {
        ConnectableDTO connectableDTO = new ConnectableDTO();
        connectableDTO.setGroupId(port.getComponent().getParentGroupId());
        connectableDTO.setName(port.getComponent().getName());
        connectableDTO.setId(port.getComponent().getId());
        connectableDTO.setType(matchPortTypeToConnectableType(port.getComponent().getType()));

        return connectableDTO;
    }

    private ConnectableDTO findConnectableComponent(
            final ProcessGroupFlowEntity flowEntity,
            final String componentName) {
        // Ports
        Optional<PortEntity> port = findPortEntityByName(
                flowEntity.getProcessGroupFlow().getFlow().getOutputPorts().stream(),
                componentName);
        if (port.isPresent()) {
            return createConnectableDTOFromPort(port.get());
        }
        port = findPortEntityByName(
                flowEntity.getProcessGroupFlow().getFlow().getInputPorts().stream(),
                componentName);
        if (port.isPresent()) {
            return createConnectableDTOFromPort(port.get());
        }

        return null;
    }

    private ProcessGroupFlowEntity advanceToNextProcessGroup(
            String processGroupName,
            ProcessGroupFlowEntity flowEntity) {
        Optional<ProcessGroupEntity> flowEntityChild = FunctionUtils.findByComponentName(
                flowEntity.getProcessGroupFlow().getFlow().getProcessGroups(),
                processGroupName);
        if (!flowEntityChild.isPresent()) {
            throw new ConfigException("Couldn't find process group '" + processGroupName + "'");
        }
        flowEntity = flowapi.getFlow(flowEntityChild.get().getId());
        return flowEntity;
    }

    private ConnectableDTO createOrFindPort(
            String destinationInputPort,
            ConnectableDTO.TypeEnum connectableType,
            ProcessGroupFlowEntity flowEntity, String processGroupName) {
        ConnectableDTO connectableDTO = findConnectableComponent(flowEntity, destinationInputPort);
        if (connectableDTO != null && connectableDTO.getType() == connectableType) {
            return connectableDTO;
        }
        if (connectableDTO == null) {
            return createConnectableDTOFromPort(createPort(
                    flowEntity.getProcessGroupFlow().getId(),
                    destinationInputPort,
                    matchConnectableTypeToPortType(connectableType)));
        }
        throw new ConfigException("'" + destinationInputPort + "' in '" + processGroupName
                + "' is not a " + connectableType);
    }

    private String determineConnectionLocation(
            ConnectableDTO source,
            ConnectableDTO destination) {
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
            ConnectableDTO source,
            ConnectableDTO dest) {
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
                connectableDTOs.add(
                        createOrFindPort(destinationInputPort, connectableType, flowEntity, processGroupName));
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

    /**
     * Create a route in NiFi composed of ports and connections.
     *
     * @param sourcePath           Path to the process group from which to create the route
     * @param destinationPath      Path to the process group to which to create the route
     * @param destinationInputPort Name of ports created along the route
     */
    public void createRoute(
            final List<String> sourcePath,
            final List<String> destinationPath,
            final String destinationInputPort) {
        ListIterator<String> source = sourcePath.listIterator();
        ListIterator<String> destination = destinationPath.listIterator();

        // Find the lowest level in the process group hierarchy where the route can pass between two process groups
        while (!source.next().equals(destination.next())) ;

        // Traverse the source branch, creating output ports down the hierarchy
        List<ConnectableDTO> sourceConnectables
                = createPorts(source, destinationInputPort, ConnectableDTO.TypeEnum.OUTPUT_PORT);
        // Reverse the sequence of the output ports as the connections should point up the hierarchy
        Collections.reverse(sourceConnectables);

        // Traverse the destination branch, creating input ports up the hierarchy
        List<ConnectableDTO> destinationConnectables
                = createPorts(destination, destinationInputPort, ConnectableDTO.TypeEnum.INPUT_PORT);

        // Stitch the two lists of connectables together
        List<ConnectableDTO> route = new ArrayList<>(sourceConnectables);
        route.addAll(destinationConnectables);
        createConnections(route.listIterator());
    }
}
