package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.model.Connection;
import com.github.hermannpencole.nifi.swagger.client.ConnectionsApi;
import com.github.hermannpencole.nifi.swagger.client.model.ConnectionDTO;
import com.github.hermannpencole.nifi.swagger.client.model.ConnectionEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static com.github.hermannpencole.nifi.config.service.TestUtils.createConnection;
import static com.github.hermannpencole.nifi.config.service.TestUtils.createConnectionEntity;
import static org.junit.Assert.assertEquals;
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
        ConnectionEntity connectionEntityTwo = createConnectionEntity("connectionTwoId", null, "sourceTwo", "destTwo", "2 GB", 10L);
        ConnectionEntity connectionEntityThee = createConnectionEntity("connectionThreeId", "connectionThree", "sourceTwo", "destOne", "1 GB", 1L);

        List<Connection> connectionsConfiguration = Arrays.asList(
                createConnection("connectionOne", "sourceOne", "destOne", "1 GB", 10L, "connectionOneId"),
                createConnection("connectionTwoId", "sourceTwo", "destTwo", "2 GB", 10L, "connectionTwoId"),
                createConnection("connectionThree", "sourceTwo", "destOne", "12 GB", 1L, "connectionThreeId")
        );
        //when
        connectionsUpdater.updateConnections(connectionsConfiguration, Arrays.asList(connectionEntityOne, connectionEntityTwo, connectionEntityThee));
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
    public void shouldMakeNoUpdatesWhenConfigurationNotFound() {
        ConnectionEntity connectionEntity = createConnectionEntity("Id", "connectionName", "sourceOne", "destOne", "1 GB", 10L);
        //when
        Connection connectionConfiguration = createConnection("connectionOtherName", "sourceOne", "destOne", "1 GB", 10L, "IdOther");
        connectionsUpdater.updateConnections(Arrays.asList(connectionConfiguration), Arrays.asList(connectionEntity));

        //then
        verify(connectionsApi, never()).updateConnection(anyString(), any(ConnectionEntity.class));
    }

}