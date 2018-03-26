package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.model.ConfigException;
import com.github.hermannpencole.nifi.config.model.ConnectionPort;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.FlowApi;
import com.github.hermannpencole.nifi.swagger.client.ProcessGroupsApi;
import com.github.hermannpencole.nifi.swagger.client.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.hermannpencole.nifi.config.utils.FunctionUtils.findByComponentName;

public class CreateRouteService {

  /**
   * The logger.
   */
  private final static Logger LOG = LoggerFactory.getLogger(CreateRouteService.class);

  @Inject
  private PortService portService;

  @Inject
  private ProcessGroupsApi processGroupsApi;

  @Inject
  private ConnectableService connectableService;

  @Inject
  private FlowApi flowapi;

  private String clientId;

  private String getClientId() {
    if (clientId == null) {
      clientId = flowapi.generateClientId();
      LOG.debug("client id generated {}", clientId);
    }
    return clientId;
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

  private ProcessGroupFlowEntity advanceToNextProcessGroup( final String processGroupName, final ProcessGroupFlowEntity flowEntity) {
    return findByComponentName(
            flowEntity.getProcessGroupFlow().getFlow().getProcessGroups(), processGroupName)
            .map(flowEntityChild -> flowapi.getFlow(flowEntityChild.getId()))
            .orElseThrow(() -> new ConfigException("Couldn't find process group '" + processGroupName + "'"));
  }

  private PortEntity createOrFindPort(final String destinationInputPort, final ConnectableDTO.TypeEnum connectableType,
          final ProcessGroupFlowEntity flowEntity) {
    ProcessGroupFlowDTO processGroupFlow = flowEntity.getProcessGroupFlow();
    Optional<PortEntity> port = portService.findPortEntityByName(processGroupFlow.getFlow(), destinationInputPort);
    if (port.isPresent()) {
      return port.get();
    } else {
      return portService.createPort(processGroupFlow.getId(), destinationInputPort,matchConnectableTypeToPortType(connectableType));
    }
  }

  private String determineConnectionLocation(final PortEntity source, final PortEntity destination) {
    switch (source.getComponent().getType()) {
      case OUTPUT_PORT:
        switch (destination.getComponent().getType()) {
          case OUTPUT_PORT:
            return destination.getComponent().getParentGroupId();
          case INPUT_PORT: default:
            return flowapi.getFlow(source.getComponent().getParentGroupId()).getProcessGroupFlow().getParentGroupId();
        }
      case INPUT_PORT:
        switch (destination.getComponent().getType()) {
          case OUTPUT_PORT:
            return source.getComponent().getParentGroupId();
          case INPUT_PORT: default:
            return source.getComponent().getParentGroupId();
        }
    }

    throw new ConfigException("Creating connections between types other than local ports not supported");
  }

  private ConnectionEntity createConnectionEntity(final PortEntity source, final PortEntity dest) {
    ConnectionEntity connectionEntity = new ConnectionEntity();
    connectionEntity.setRevision(new RevisionDTO());
    connectionEntity.getRevision().setVersion(0L);
    connectionEntity.getRevision().setClientId(getClientId());
    connectionEntity.setComponent(new ConnectionDTO());
    connectionEntity.getComponent().setSource(connectableService.createConnectableDTOFromPort(source));
    connectionEntity.getComponent().setDestination(connectableService.createConnectableDTOFromPort(dest));
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

  private List<PortEntity> createPorts(
          final ListIterator<String> branch,
          final String destinationInputPort,
          final ConnectableDTO.TypeEnum connectableType)
          throws ApiException {
    List<PortEntity> connectableDTOs = new ArrayList<>();
    int mergeLevel = branch.previousIndex();
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
        connectableDTOs.add( createOrFindPort(destinationInputPort, connectableType, flowEntity) );
      }
    }

    return connectableDTOs;
  }

  private void createConnections(final ListIterator<PortEntity> connectables) {
    if (!connectables.hasNext()) return;

    PortEntity current;
    PortEntity next = connectables.next();

    while (connectables.hasNext()) {
      current = next;
      next = connectables.next();
      ProcessGroupFlowEntity flowEntity = flowapi.getFlow(determineConnectionLocation(current, next));
      ConnectionEntity connectionEntity = createConnectionEntity(current, next);

      if (!connectionExists( flowEntity.getProcessGroupFlow().getFlow().getConnections().stream(), connectionEntity)) {
        processGroupsApi.createConnection(flowEntity.getProcessGroupFlow().getId(), connectionEntity);
      }
    }
  }

  private void startPorts(Stream<PortEntity> connectables) {
    connectables.map(port -> portService.getById(port.getId(), port.getComponent().getType()))
            .forEach(port -> portService.setState(port, PortDTO.StateEnum.RUNNING ));
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
    while (source.next().equals(destination.next())) ;

    // Traverse the source branch, creating output ports down the hierarchy
    List<PortEntity> sourceConnectables = createPorts(source, name, ConnectableDTO.TypeEnum.OUTPUT_PORT);
    // Reverse the sequence of the output ports as the connections should point up the hierarchy
    Collections.reverse(sourceConnectables);

    // Traverse the destination branch, creating input ports up the hierarchy
    List<PortEntity> destinationConnectables = createPorts(destination, name, ConnectableDTO.TypeEnum.INPUT_PORT);

    // Switch the two lists of connectables together
    List<PortEntity> route = new ArrayList<>(sourceConnectables);
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
   * @param optionNoStartProcessors Whether or not to start ports created along the route
   * @throws IOException If repository file cannot be found
   */
  public void createRoutes(List<ConnectionPort> connections, boolean optionNoStartProcessors) throws IOException {
      for (ConnectionPort routeConnectionEntity : connections) {
        createRoute(
                routeConnectionEntity.getName(),
                Arrays.stream(routeConnectionEntity.getSource().split(">")).map(String::trim).collect(Collectors.toList()),
                Arrays.stream(routeConnectionEntity.getDestination().split(">")).map(String::trim).collect(Collectors.toList()),
                !optionNoStartProcessors);
      }

  }
}
