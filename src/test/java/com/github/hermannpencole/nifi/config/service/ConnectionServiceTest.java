package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.model.TimeoutException;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.ConnectionsApi;
import com.github.hermannpencole.nifi.swagger.client.FlowfileQueuesApi;
import com.github.hermannpencole.nifi.swagger.client.model.*;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.mockito.Mockito.when;

/**
 * API tests for AccessApi
 */
@RunWith(MockitoJUnitRunner.class)
public class ConnectionServiceTest {

    @Mock
    private ConnectionsApi connectionsApiMock;

    @Mock
    private FlowfileQueuesApi flowfileQueuesApiMock;

    @Test
    public void waitEmptyQueueTest() throws ApiException, IOException, URISyntaxException {
        Injector injector = Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(ConnectionsApi.class).toInstance(connectionsApiMock);
                bind(FlowfileQueuesApi.class).toInstance(flowfileQueuesApiMock);
                bind(Integer.class).annotatedWith(Names.named("timeout")).toInstance(1);
                bind(Integer.class).annotatedWith(Names.named("interval")).toInstance(1);
                bind(Boolean.class).annotatedWith(Names.named("forceMode")).toInstance(false);
            }
        });
        ConnectionService connectionService = injector.getInstance(ConnectionService.class);
        ConnectionEntity connection = TestUtils.createConnectionEntity("id","sourceId","destinationId");
        connection.setStatus(new ConnectionStatusDTO());
        connection.getStatus().setAggregateSnapshot(new ConnectionStatusSnapshotDTO());
        connection.getStatus().getAggregateSnapshot().setQueuedCount("0");
        connection.getStatus().getAggregateSnapshot().setQueuedSize("0");
        when(connectionsApiMock.getConnection("id")).thenReturn(connection);
        connectionService.waitEmptyQueue(connection);
    }

    @Test
    public void waitEmptyQueueForceTest() throws ApiException, IOException, URISyntaxException {
        Injector injector = Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(ConnectionsApi.class).toInstance(connectionsApiMock);
                bind(FlowfileQueuesApi.class).toInstance(flowfileQueuesApiMock);
                bind(Integer.class).annotatedWith(Names.named("timeout")).toInstance(1);
                bind(Integer.class).annotatedWith(Names.named("interval")).toInstance(1);
                bind(Boolean.class).annotatedWith(Names.named("forceMode")).toInstance(true);
            }
        });
        ConnectionService connectionService = injector.getInstance(ConnectionService.class);
        ConnectionEntity connection = TestUtils.createConnectionEntity("id","sourceId","destinationId");
        connection.setStatus(new ConnectionStatusDTO());
        connection.getStatus().setAggregateSnapshot(new ConnectionStatusSnapshotDTO());
        connection.getStatus().getAggregateSnapshot().setQueuedCount("1");
        connection.getStatus().getAggregateSnapshot().setQueuedSize("0");
        when(connectionsApiMock.getConnection("id")).thenReturn(connection);

        DropRequestEntity dropRequest = new DropRequestEntity();
        dropRequest.setDropRequest(new DropRequestDTO());
        dropRequest.getDropRequest().setId("idDrop");
        dropRequest.getDropRequest().setFinished(true);
        when(flowfileQueuesApiMock.createDropRequest("id")).thenReturn(dropRequest);
        when(flowfileQueuesApiMock.getDropRequest("id", dropRequest.getDropRequest().getId())).thenReturn(dropRequest);
        when(flowfileQueuesApiMock.removeDropRequest("id", dropRequest.getDropRequest().getId())).thenReturn(dropRequest);

        connectionService.waitEmptyQueue(connection);
    }

    @Test(expected = TimeoutException.class)
    public void waitEmptyQueueTimeOutTest() throws ApiException, IOException, URISyntaxException {
        Injector injector = Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(ConnectionsApi.class).toInstance(connectionsApiMock);
                bind(FlowfileQueuesApi.class).toInstance(flowfileQueuesApiMock);
                bind(Integer.class).annotatedWith(Names.named("timeout")).toInstance(1);
                bind(Integer.class).annotatedWith(Names.named("interval")).toInstance(1);
                bind(Boolean.class).annotatedWith(Names.named("forceMode")).toInstance(false);
            }
        });
        ConnectionService connectionService = injector.getInstance(ConnectionService.class);
        ConnectionEntity connection = TestUtils.createConnectionEntity("id","sourceId","destinationId");
        connection.setStatus(new ConnectionStatusDTO());
        connection.getStatus().setAggregateSnapshot(new ConnectionStatusSnapshotDTO());
        connection.getStatus().getAggregateSnapshot().setQueuedCount("1");
        connection.getStatus().getAggregateSnapshot().setQueuedSize("0");
        when(connectionsApiMock.getConnection("id")).thenReturn(connection);
        connectionService.waitEmptyQueue(connection);
    }
}