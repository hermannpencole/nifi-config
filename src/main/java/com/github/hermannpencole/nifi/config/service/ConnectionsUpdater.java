package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.model.Connection;
import com.github.hermannpencole.nifi.swagger.client.ConnectionsApi;
import com.github.hermannpencole.nifi.swagger.client.FlowApi;
import com.github.hermannpencole.nifi.swagger.client.ProcessGroupsApi;
import com.github.hermannpencole.nifi.swagger.client.model.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class ConnectionsUpdater {

    @Inject
    private ConnectionsApi connectionsApi;

    @Inject
    private ProcessGroupsApi processGroupsApi;

    @Inject
    private FlowApi flowapi;

    /**
     * update connection and create if not exist
     *
     * @param connectionsConfiguration
     * @param componentSearch
     */
    public void updateConnections(List<Connection> connectionsConfiguration, ProcessGroupFlowEntity componentSearch) { //List<ConnectionEntity> currentConnections, List<ProcessorEntity> processors, String id) {
        List<ConnectionEntity> currentConnections = componentSearch.getProcessGroupFlow().getFlow().getConnections();
        Map<String, Connection> connectionMap = connectionsConfiguration
                .stream()
                .collect(Collectors.toMap(Connection::getName, Function.identity()));

        currentConnections.forEach(
                entity -> {
                    ConnectionDTO connectionDTO = entity.getComponent();
                    Connection config = connectionMap.getOrDefault(connectionDTO.getName(), null);
                    if (config != null) {
                        connectionMap.remove(connectionDTO.getName());
                        connectionDTO.setBackPressureObjectThreshold(config.getBackPressureObjectThreshold());
                        connectionDTO.setBackPressureDataSizeThreshold(config.getBackPressureDataSizeThreshold());
                        connectionDTO.setFlowFileExpiration(config.getFlowFileExpiration());
                        connectionsApi.updateConnection(entity.getId(), entity);
                    }
                });
        //create if not exist
        connectionMap.keySet().forEach( key -> {
            Connection config = connectionMap.get(key);
            ConnectionEntity connection = new ConnectionEntity();
            ConnectionDTO connectionDTO = new ConnectionDTO();
            Optional<ConnectableDTO> destination = findByName(componentSearch, config.getDestination());
            if (!destination.isPresent()) throw new RuntimeException("Cannot find " + config.getDestination() + " for create connection " + config.getName());
            connectionDTO.setDestination(destination.get());
            Optional<ConnectableDTO> source = findByName(componentSearch, config.getSource());
            if (!source.isPresent()) throw new RuntimeException("Cannot find " + config.getSource() + " for create connection " + config.getName());
            connectionDTO.setSource(source.get());
            connectionDTO.setName(config.getName());
            connectionDTO.setBackPressureObjectThreshold(config.getBackPressureObjectThreshold());
            connectionDTO.setBackPressureDataSizeThreshold(config.getBackPressureDataSizeThreshold());
            connectionDTO.setSelectedRelationships(config.getRelationShips());
            connectionDTO.setFlowFileExpiration(config.getFlowFileExpiration());
            connection.setComponent(connectionDTO);
            connection.setRevision(new RevisionDTO());
            connection.getRevision().setVersion(0L);
            processGroupsApi.createConnection(componentSearch.getProcessGroupFlow().getId(), connection);
        });

    }

    /**
     * find processor, inputport, ouput port funnel or remote processor by id in flow
     *
     * @param componentSearch the compenant whre search
     * @param name the name to find
     * @return an optionnal find
     */
    public Optional<ConnectableDTO> findByName(ProcessGroupFlowEntity componentSearch, String name){
        if (name.contains(":")) {
                Optional<ProcessGroupEntity> processGroupEntity = componentSearch.getProcessGroupFlow().getFlow().getProcessGroups().stream().filter(processor ->  name.split(":")[0].equals(processor.getComponent().getName())).findFirst();
                if (!processGroupEntity.isPresent()) return Optional.empty();
                return findByName(flowapi.getFlow(processGroupEntity.get().getId()), name.split(":")[1]);
        }
        Optional<ConnectableDTO> result = componentSearch.getProcessGroupFlow().getFlow().getProcessors().stream().filter(processor -> name.equals(processor.getComponent().getName())).findFirst()
                .map(p -> {
                    ConnectableDTO connectableDTO = new ConnectableDTO();
                    connectableDTO.setGroupId(componentSearch.getProcessGroupFlow().getId());
                    connectableDTO.setId(p.getId());
                    connectableDTO.setType(ConnectableDTO.TypeEnum.PROCESSOR);
                    return connectableDTO;
                });
        if (!result.isPresent())
            result = componentSearch.getProcessGroupFlow().getFlow().getInputPorts().stream().filter(port -> name.equals(port.getComponent().getName())).findFirst()
                    .map(p -> {
                        ConnectableDTO connectableDTO = new ConnectableDTO();
                        connectableDTO.setGroupId(componentSearch.getProcessGroupFlow().getId());
                        connectableDTO.setId(p.getId());
                        connectableDTO.setType(ConnectableDTO.TypeEnum.INPUT_PORT);
                        return connectableDTO;
                    });
        if (!result.isPresent())
            result = componentSearch.getProcessGroupFlow().getFlow().getOutputPorts().stream().filter(port -> name.equals(port.getComponent().getName())).findFirst()
                    .map(p -> {
                        ConnectableDTO connectableDTO = new ConnectableDTO();
                        connectableDTO.setGroupId(componentSearch.getProcessGroupFlow().getId());
                        connectableDTO.setId(p.getId());
                        connectableDTO.setType(ConnectableDTO.TypeEnum.OUTPUT_PORT);
                        return connectableDTO;
                    });
       /* if (!result.isPresent())
            result = flow.getFunnels().stream().filter(funnel -> name.equals(funnel.getId())).findFirst();*/
        if (!result.isPresent())
            result = componentSearch.getProcessGroupFlow().getFlow().getRemoteProcessGroups().stream().filter(remoteProcessGroup -> name.equals(remoteProcessGroup.getComponent().getName())).findFirst()
                    .map(p -> {
                        ConnectableDTO connectableDTO = new ConnectableDTO();
                        connectableDTO.setGroupId(componentSearch.getProcessGroupFlow().getId());
                        connectableDTO.setId(p.getId());
                        connectableDTO.setType(ConnectableDTO.TypeEnum.PROCESSOR);
                        return connectableDTO;
                    });
        return result;
    }

}
