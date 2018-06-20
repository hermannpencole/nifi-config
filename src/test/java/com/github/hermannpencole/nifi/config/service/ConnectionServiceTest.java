package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.model.TimeoutException;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.ConnectionsApi;
import com.github.hermannpencole.nifi.swagger.client.FlowApi;
import com.github.hermannpencole.nifi.swagger.client.FlowfileQueuesApi;
import com.github.hermannpencole.nifi.swagger.client.ProcessGroupsApi;
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

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * API tests for AccessApi
 */
@RunWith(MockitoJUnitRunner.class)
public class ConnectionServiceTest {

    @Mock
    private ConnectionsApi connectionsApiMock;

    @Mock
    private FlowfileQueuesApi flowfileQueuesApiMock;

    @Mock
    private FlowApi flowApiMock;

    @Mock
    private ProcessGroupsApi processGroupsApiMock;

    @Mock
    private ProcessorService processorServiceMock;

    @Mock
    private PortService portServiceMock;

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

    @Test
    public void removeExternalConnectionTest() throws ApiException, IOException, URISyntaxException {
        Injector injector = Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(ConnectionsApi.class).toInstance(connectionsApiMock);
                bind(FlowfileQueuesApi.class).toInstance(flowfileQueuesApiMock);
                bind(FlowApi.class).toInstance(flowApiMock);
                bind(ProcessGroupsApi.class).toInstance(processGroupsApiMock);
                bind(ProcessorService.class).toInstance(processorServiceMock);
                bind(PortService.class).toInstance(portServiceMock);
                bind(Integer.class).annotatedWith(Names.named("timeout")).toInstance(1);
                bind(Integer.class).annotatedWith(Names.named("interval")).toInstance(1);
                bind(Boolean.class).annotatedWith(Names.named("forceMode")).toInstance(false);
            }
        });
        ConnectionService connectionService = injector.getInstance(ConnectionService.class);

        ProcessGroupFlowEntity flow = TestUtils.createProcessGroupFlowEntity("123", "flow1");
        flow.getProcessGroupFlow().setParentGroupId("456");
        when(flowApiMock.getFlow("345")).thenReturn(flow);

        ConnectionsEntity connectionsEntity = new ConnectionsEntity();
        connectionsEntity.setConnections(asList(
                createConnection("conn1", "s1", "d1", "345", "000"),
                createConnection("conn1", "s2", "d2", "000", "345"),
                createConnection("conn1", "s3", "d3", "000", "000")
        ));
        when(processGroupsApiMock.getConnections("456")).thenReturn(connectionsEntity);

        ProcessGroupEntity processGroupEntity = TestUtils.createProcessGroupEntity("345", "group");
        connectionService.removeExternalConnections(processGroupEntity);

        verify(flowApiMock).getFlow(eq("345"));
        verify(connectionsApiMock, times(2)).deleteConnection(any(), any(), any());
    }

    private ConnectionEntity createConnection(String id, String sourceId, String destinationId,
                                              String sourceGroupId, String destinationGroupId) {
        ConnectionEntity conn = TestUtils.createConnectionEntity(id, sourceId , destinationId);
        conn.setSourceGroupId(sourceGroupId);
        conn.setDestinationGroupId(destinationGroupId);
        conn.setComponent(new ConnectionDTO());
        conn.getComponent().setId(id);
        return conn;
    }
}