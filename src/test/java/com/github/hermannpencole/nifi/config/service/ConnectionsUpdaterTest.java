package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.model.Connection;
import com.github.hermannpencole.nifi.swagger.client.ConnectionsApi;
import com.github.hermannpencole.nifi.swagger.client.model.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.github.hermannpencole.nifi.config.service.TestUtils.createConnection;
import static com.github.hermannpencole.nifi.config.service.TestUtils.createConnectionEntity;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ConnectionsUpdaterTest {

    @Mock
    ConnectionsApi connectionsApi;
    @InjectMocks
    ConnectionsUpdater connectionsUpdater;

    @Test
    public void shouldUpdateConnections() {
        ConnectionEntity connectionEntityOne = createConnectionEntity("connectionOneId", "connectionOne", "sourceOne", "destOne", "1 GB", 10L);
        ConnectionEntity connectionEntityTwo = createConnectionEntity("connectionTwoId", "connectionTwoId", "sourceTwo", "destTwo", "2 GB", 10L);
        ConnectionEntity connectionEntityThee = createConnectionEntity("connectionThreeId", "connectionThree", "sourceTwo", "destOne", "1 GB", 1L);

        List<Connection> connectionsConfiguration = Arrays.asList(
                createConnection("connectionOne", "sourceOne", "destOne", "1 GB", 10L, "connectionOneId"),
                createConnection("connectionTwoId", "sourceTwo", "destTwo", "2 GB", 10L, "connectionTwoId"),
                createConnection("connectionThree", "sourceTwo", "destOne", "12 GB", 1L, "connectionThreeId")
        );
        //when
        ProcessGroupFlowEntity processGroupFlowEntity = new ProcessGroupFlowEntity();
        processGroupFlowEntity.setProcessGroupFlow(new ProcessGroupFlowDTO());
        processGroupFlowEntity.getProcessGroupFlow().setFlow(new FlowDTO());
        processGroupFlowEntity.getProcessGroupFlow().setId("idgroup");
        processGroupFlowEntity.getProcessGroupFlow().getFlow().setProcessGroups(new ArrayList<>());
        processGroupFlowEntity.getProcessGroupFlow().getFlow().setRemoteProcessGroups(new ArrayList<>());
        processGroupFlowEntity.getProcessGroupFlow().getFlow().setOutputPorts(new ArrayList<>());
        processGroupFlowEntity.getProcessGroupFlow().getFlow().setInputPorts(new ArrayList<>());
        processGroupFlowEntity.getProcessGroupFlow().getFlow().setProcessors(new ArrayList<>());
        processGroupFlowEntity.getProcessGroupFlow().getFlow().setConnections(Arrays.asList(connectionEntityOne, connectionEntityTwo, connectionEntityThee));
        connectionsUpdater.updateConnections(connectionsConfiguration, processGroupFlowEntity);
        //then
        ConnectionDTO connectionDTOOne = connectionEntityOne.getComponent();
        assertEquals("1 GB", connectionDTOOne.getBackPressureDataSizeThreshold());
        assertEquals(10L, connectionDTOOne.getBackPressureObjectThreshold().longValue());
        ConnectionDTO connectionDTOTwo = connectionEntityTwo.getComponent();
        assertEquals("2 GB", connectionDTOTwo.getBackPressureDataSizeThreshold());
        assertEquals(10L, connectionDTOTwo.getBackPressureObjectThreshold().longValue());
        ConnectionDTO connectionDTOThree = connectionEntityThee.getComponent();
        assertEquals("12 GB", connectionDTOThree.getBackPressureDataSizeThreshold());
        assertEquals(1L, connectionDTOThree.getBackPressureObjectThreshold().longValue());

        verify(connectionsApi, times(1)).updateConnection(connectionEntityOne.getId(), connectionEntityOne);
        verify(connectionsApi, times(1)).updateConnection(connectionEntityTwo.getId(), connectionEntityTwo);
        verify(connectionsApi, times(1)).updateConnection(connectionEntityThee.getId(), connectionEntityThee);
    }

    @Test
    public void shouldThrowExceptionWhenConfigurationNotFound() {
        ConnectionEntity connectionEntity = createConnectionEntity("Id", "connectionName", "sourceOne", "destOne", "1 GB", 10L);
        //when
        Connection connectionConfiguration = createConnection("connectionOtherName", "sourceOne", "destOne", "1 GB", 10L, "IdOther");
        ProcessGroupFlowEntity processGroupFlowEntity = new ProcessGroupFlowEntity();
        processGroupFlowEntity.setProcessGroupFlow(new ProcessGroupFlowDTO());
        processGroupFlowEntity.getProcessGroupFlow().setFlow(new FlowDTO());
        processGroupFlowEntity.getProcessGroupFlow().getFlow().setConnections(Arrays.asList(connectionEntity));
        processGroupFlowEntity.getProcessGroupFlow().setId("id");
        processGroupFlowEntity.getProcessGroupFlow().getFlow().setProcessors(new ArrayList<>());
        processGroupFlowEntity.getProcessGroupFlow().getFlow().setProcessGroups(new ArrayList<>());
        processGroupFlowEntity.getProcessGroupFlow().getFlow().setInputPorts(new ArrayList<>());
        processGroupFlowEntity.getProcessGroupFlow().getFlow().setOutputPorts(new ArrayList<>());
        processGroupFlowEntity.getProcessGroupFlow().getFlow().setRemoteProcessGroups(new ArrayList<>());
        try {
            connectionsUpdater.updateConnections(Arrays.asList(connectionConfiguration), processGroupFlowEntity);
            fail("Exception must be throw");
        } catch (Exception e) {
            //then
            verify(connectionsApi, never()).updateConnection(anyString(), any(ConnectionEntity.class));
            assertEquals("Cannot find destOne for create connection connectionOtherName", e.getMessage());
        }

    }

}