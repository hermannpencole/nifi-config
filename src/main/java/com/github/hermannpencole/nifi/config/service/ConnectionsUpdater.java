package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.model.Connection;
import com.github.hermannpencole.nifi.swagger.client.ConnectionsApi;
import com.github.hermannpencole.nifi.swagger.client.model.ConnectionDTO;
import com.github.hermannpencole.nifi.swagger.client.model.ConnectionEntity;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class ConnectionsUpdater {

    @Inject
    private ConnectionsApi connectionsApi;

    public void updateConnections(List<Connection> connectionsConfiguration, List<ConnectionEntity> currentConnections) {

        Map<String, Connection> connectionMap = connectionsConfiguration
                .stream()
                .collect(Collectors.toMap(Connection::getId, Function.identity()));

        currentConnections.forEach(
                entity -> {
                    ConnectionDTO connectionDTO = entity.getComponent();
                    Connection config = connectionMap.getOrDefault(connectionDTO.getId(), null);
                    if (config != null) {
                        connectionDTO.setBackPressureObjectThreshold(config.getBackPressureObjectThreshold());
                        connectionDTO.setBackPressureDataSizeThreshold(config.getBackPressureDataSizeThreshold());
                        connectionDTO.setSelectedRelationships(null);
                        connectionsApi.updateConnection(entity.getId(), entity);
                    }
                });
    }

}
