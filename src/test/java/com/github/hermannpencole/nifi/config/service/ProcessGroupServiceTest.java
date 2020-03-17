package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.Main;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.FlowApi;
import com.github.hermannpencole.nifi.swagger.client.ProcessGroupsApi;
import com.github.hermannpencole.nifi.swagger.client.model.*;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * API tests for AccessApi
 */
@RunWith(MockitoJUnitRunner.class)
public class ProcessGroupServiceTest {
    @Mock
    private FlowApi flowApiMock;

    @Mock
    private ProcessGroupsApi processGroupsApiMock;

    @Mock
    private ProcessorService processorServiceMock;

    @Mock
    private ConnectionService connectionServiceMock;

    @InjectMocks
    private ProcessGroupService processGroupService;

    @Test
    public void changeDirectoryNotExitingTest() throws ApiException, IOException, URISyntaxException {
        List<String> branch = Arrays.asList("root", "elt2");

        ProcessGroupFlowEntity responseRoot = TestUtils.createProcessGroupFlowEntity("root", "root");
        responseRoot.getProcessGroupFlow().getFlow().setProcessGroups(new ArrayList<>());
        responseRoot.getProcessGroupFlow().getFlow()
                .getProcessGroups().add(TestUtils.createProcessGroupEntity("idElt1", "elt1"));
        when(flowApiMock.getFlow(responseRoot.getProcessGroupFlow().getId())).thenReturn(responseRoot);

        Optional<ProcessGroupFlowEntity> response = processGroupService.changeDirectory(branch);
        assertFalse(response.isPresent());
     }

    @Test
    public void changeDirectoryExitingBranchTest() throws ApiException, IOException, URISyntaxException {
        List<String> branch = Arrays.asList("root", "elt1");

        ProcessGroupFlowEntity responseRoot = TestUtils.createProcessGroupFlowEntity("root", "root");
        responseRoot.getProcessGroupFlow().getFlow().setProcessGroups(new ArrayList<>());
        responseRoot.getProcessGroupFlow().getFlow()
                .getProcessGroups().add(TestUtils.createProcessGroupEntity("idElt1", "elt1"));
        when(flowApiMock.getFlow(responseRoot.getProcessGroupFlow().getId())).thenReturn(responseRoot);
        ProcessGroupFlowEntity responseElt = TestUtils.createProcessGroupFlowEntity("idElt1", "elt1");
        when(flowApiMock.getFlow(responseElt.getProcessGroupFlow().getId())).thenReturn(responseElt);

        Optional<ProcessGroupFlowEntity> response = processGroupService.changeDirectory(branch);
        assertTrue(response.isPresent());
        assertEquals("idElt1", response.get().getProcessGroupFlow().getId());
    }

    @Test
    public void createDirectoryNotExitingTest() throws ApiException, IOException, URISyntaxException {
        List<String> branch = Arrays.asList("root", "elt2");

        ProcessGroupFlowEntity responseRoot = TestUtils.createProcessGroupFlowEntity("root", "root");
        responseRoot.getProcessGroupFlow().getFlow().setProcessGroups(new ArrayList<>());
        responseRoot.getProcessGroupFlow().getFlow()
                .getProcessGroups().add(TestUtils.createProcessGroupEntity("idElt1", "elt1"));
        when(flowApiMock.getFlow(responseRoot.getProcessGroupFlow().getId())).thenReturn(responseRoot);
        ProcessGroupFlowEntity responseElt = TestUtils.createProcessGroupFlowEntity("idElt2", "elt2");
        when(processGroupsApiMock.createProcessGroup(any(), any())).thenReturn(TestUtils.createProcessGroupEntity("idElt2", "elt2"));
        when(flowApiMock.getFlow(responseElt.getProcessGroupFlow().getId())).thenReturn(responseElt);

        Injector injector = Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(ProcessGroupService.class).toInstance(processGroupService);
                bind(ProcessGroupsApi.class).toInstance(processGroupsApiMock);
                bind(ProcessorService.class).toInstance(processorServiceMock);
                bind(ConnectionService.class).toInstance(connectionServiceMock);
                bind(FlowApi.class).toInstance(flowApiMock);
                bind(Integer.class).annotatedWith(Names.named("timeout")).toInstance(1);
                bind(Integer.class).annotatedWith(Names.named("interval")).toInstance(1);
                bind(Boolean.class).annotatedWith(Names.named("forceMode")).toInstance(false);
                bind(Double.class).annotatedWith(Names.named("placeWidth")).toInstance(1200d);
                bind(PositionDTO.class).annotatedWith(Names.named("startPosition")).toInstance(Main.createPosition("0,0"));
            }
        });
        ProcessGroupFlowEntity response = injector.getInstance(ProcessGroupService.class).createDirectory(branch);
        assertEquals("idElt2", response.getProcessGroupFlow().getId());
    }

    @Test
    public void createDirectoryExitingBranchTest() throws ApiException, IOException, URISyntaxException {
        List<String> branch = Arrays.asList("root", "elt1");

        ProcessGroupFlowEntity responseRoot = TestUtils.createProcessGroupFlowEntity("root", "root");
        responseRoot.getProcessGroupFlow().getFlow().setProcessGroups(new ArrayList<>());
        responseRoot.getProcessGroupFlow().getFlow()
                .getProcessGroups().add(TestUtils.createProcessGroupEntity("idElt1", "elt1"));
        when(flowApiMock.getFlow(responseRoot.getProcessGroupFlow().getId())).thenReturn(responseRoot);
        ProcessGroupFlowEntity responseElt = TestUtils.createProcessGroupFlowEntity("idElt1", "elt1");
        when(flowApiMock.getFlow(responseElt.getProcessGroupFlow().getId())).thenReturn(responseElt);

        ProcessGroupFlowEntity response = processGroupService.createDirectory(branch);
        assertEquals("idElt1", response.getProcessGroupFlow().getId());
    }

    @Test
    public void setStateTest() throws ApiException, IOException, URISyntaxException {
        ProcessGroupFlowEntity responseRoot = TestUtils.createProcessGroupFlowEntity("root", "rootName");
        processGroupService.setState(responseRoot.getProcessGroupFlow().getId(), ScheduleComponentsEntity.StateEnum.RUNNING);
        ScheduleComponentsEntity body = new ScheduleComponentsEntity();
        body.setId("root");
        body.setState(ScheduleComponentsEntity.StateEnum.RUNNING);
        body.setComponents(null);//for all
        verify(flowApiMock).scheduleComponents("root", body);
    }

    @Test
    public void startTest() throws ApiException, IOException, URISyntaxException {
        ProcessGroupFlowEntity responseRoot = TestUtils.createProcessGroupFlowEntity("root", "rootName");
        List<ConnectionEntity> connections = new ArrayList<>();
        connections.add(TestUtils.createConnectionEntity("idCnx", "idProc","idProc2"));
        responseRoot.getProcessGroupFlow().getFlow().setConnections(connections);
        responseRoot.getProcessGroupFlow().getFlow().setProcessors(new ArrayList<>());
        responseRoot.getProcessGroupFlow().getFlow().setProcessGroups(new ArrayList<>());
        responseRoot.getProcessGroupFlow().getFlow()
                .getProcessors().add(TestUtils.createProcessorEntity("idProc","nameProc") );
        responseRoot.getProcessGroupFlow().getFlow()
                .getProcessors().add(TestUtils.createProcessorEntity("idProc2","nameProc2") );
        processGroupService.start(responseRoot);
        ArgumentCaptor<ProcessorEntity> processorCapture = ArgumentCaptor.forClass(ProcessorEntity.class);
        verify(processorServiceMock, times(2)).setState(processorCapture.capture(), eq(ProcessorDTO.StateEnum.RUNNING));
        assertEquals("idProc2", processorCapture.getAllValues().get(0).getId());
        assertEquals("idProc", processorCapture.getAllValues().get(1).getId());
    }

    @Test
    public void stopTest() throws ApiException, IOException, URISyntaxException {
        ProcessGroupFlowEntity responseRoot = TestUtils.createProcessGroupFlowEntity("root", "rootName");
        List<ConnectionEntity> connections = new ArrayList<>();
        connections.add(TestUtils.createConnectionEntity("idCnx", "idProc","idProc2"));
        responseRoot.getProcessGroupFlow().getFlow().setConnections(connections);
        responseRoot.getProcessGroupFlow().getFlow().setProcessors(new ArrayList<>());
        responseRoot.getProcessGroupFlow().getFlow().setProcessGroups(new ArrayList<>());
        responseRoot.getProcessGroupFlow().getFlow()
                .getProcessors().add(TestUtils.createProcessorEntity("idProc","nameProc") );
        responseRoot.getProcessGroupFlow().getFlow()
                .getProcessors().add(TestUtils.createProcessorEntity("idProc2","nameProc2") );
        when(connectionServiceMock.isEmptyQueue(any())).thenReturn(false).thenReturn(true);
        processGroupService.stop(responseRoot);
        ArgumentCaptor<ProcessorEntity> processorCapture = ArgumentCaptor.forClass(ProcessorEntity.class);
        verify(processorServiceMock, times(2)).setState(processorCapture.capture(), eq(ProcessorDTO.StateEnum.STOPPED));
        assertEquals("idProc", processorCapture.getAllValues().get(0).getId());
        assertEquals("idProc2", processorCapture.getAllValues().get(1).getId());
        ArgumentCaptor<ConnectionEntity> connectionCapture = ArgumentCaptor.forClass(ConnectionEntity.class);
        verify(connectionServiceMock).waitEmptyQueue(connectionCapture.capture());
        assertEquals("idProc", connectionCapture.getValue().getSourceId());
    }

    @Test
    public void deleteTest() throws ApiException, IOException, URISyntaxException {
        final ProcessGroupEntity processGroupEntity = TestUtils.createProcessGroupEntity("idElt1", "elt1");
        when(processGroupsApiMock.getProcessGroup(eq("123"))).thenReturn(processGroupEntity);
        when(processGroupsApiMock.removeProcessGroup(eq("123"), any(), any())).thenReturn(processGroupEntity);

        Injector injector = Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(ProcessGroupService.class).toInstance(processGroupService);
                bind(ProcessGroupsApi.class).toInstance(processGroupsApiMock);
                bind(ProcessorService.class).toInstance(processorServiceMock);
                bind(ConnectionService.class).toInstance(connectionServiceMock);
                bind(FlowApi.class).toInstance(flowApiMock);
                bind(Integer.class).annotatedWith(Names.named("timeout")).toInstance(10);
                bind(Integer.class).annotatedWith(Names.named("interval")).toInstance(1);
                bind(Boolean.class).annotatedWith(Names.named("forceMode")).toInstance(false);
                bind(Double.class).annotatedWith(Names.named("placeWidth")).toInstance(1200d);
                bind(PositionDTO.class).annotatedWith(Names.named("startPosition")).toInstance(Main.createPosition("0,0"));
            }
        });
        ProcessGroupService service = injector.getInstance(ProcessGroupService.class);

        service.delete("123");

        verify(processGroupsApiMock).getProcessGroup(eq("123"));
        verify(processGroupsApiMock).removeProcessGroup(eq("123"), any(), any());
        verifyZeroInteractions(connectionServiceMock);
    }

    @Test
    public void deleteExternalConnectionTest() throws ApiException, IOException, URISyntaxException {
        final ProcessGroupEntity processGroupEntity = TestUtils.createProcessGroupEntity("idElt1", "elt1");
        when(processGroupsApiMock.getProcessGroup(eq("123"))).thenReturn(processGroupEntity);
        final ApiException externalConnectionException = new ApiException(1, "Error", null, "Cannot delete Process Group because " +
                "Input Port 3d3a458f-7365-1c62-aa8f-864fe0bf3085 has at least one incoming connection from a " +
                "component outside of the Process Group. Delete this connection first.");
        when(processGroupsApiMock.removeProcessGroup(eq("123"), any(), any()))
                .thenThrow(externalConnectionException)
                .thenReturn(processGroupEntity);

        Injector injector = Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(ProcessGroupService.class).toInstance(processGroupService);
                bind(ProcessGroupsApi.class).toInstance(processGroupsApiMock);
                bind(ProcessorService.class).toInstance(processorServiceMock);
                bind(ConnectionService.class).toInstance(connectionServiceMock);
                bind(FlowApi.class).toInstance(flowApiMock);
                bind(Integer.class).annotatedWith(Names.named("timeout")).toInstance(10);
                bind(Integer.class).annotatedWith(Names.named("interval")).toInstance(1);
                bind(Boolean.class).annotatedWith(Names.named("forceMode")).toInstance(false);
                bind(Double.class).annotatedWith(Names.named("placeWidth")).toInstance(1200d);
                bind(PositionDTO.class).annotatedWith(Names.named("startPosition")).toInstance(Main.createPosition("0,0"));
            }
        });
        ProcessGroupService service = injector.getInstance(ProcessGroupService.class);

        service.delete("123");

        verify(processGroupsApiMock, times(2)).getProcessGroup(eq("123"));
        verify(processGroupsApiMock, times(2)).removeProcessGroup(eq("123"), any(), any());
        verify(connectionServiceMock).removeExternalConnections(eq(processGroupEntity));
    }

    @Test
//    1 - 2
//    2 - 7
//    3 - 4
//    4 - 5
//    4 - 6
//    6 - 7
//
//    1,3 - 2,4 - 5,6 - 7
    public void reorderTest() throws ApiException, IOException, URISyntaxException {
        ProcessGroupFlowEntity responseRoot = TestUtils.createProcessGroupFlowEntity("root", "rootName");
        List<ConnectionEntity> connections = new ArrayList<>();
        connections.add(TestUtils.createConnectionEntity("idCnx1", "1","2"));
        connections.add(TestUtils.createConnectionEntity("idCnx2", "2","7"));
        connections.add(TestUtils.createConnectionEntity("idCnx3", "3","4"));
        connections.add(TestUtils.createConnectionEntity("idCnx4", "4","5"));
        connections.add(TestUtils.createConnectionEntity("idCnx5", "4","6"));
        connections.add(TestUtils.createConnectionEntity("idCnx6", "6","7"));
        responseRoot.getProcessGroupFlow().getFlow().setConnections(connections);
        List<ProcessorEntity> processors = new ArrayList<>();
        processors = new ArrayList<>();
        processors.add(TestUtils.createProcessorEntity("1","name1"));
        processors.add(TestUtils.createProcessorEntity("2","name2"));
        processors.add(TestUtils.createProcessorEntity("3","name3"));
        processors.add(TestUtils.createProcessorEntity("4","name4"));
        processors.add(TestUtils.createProcessorEntity("5","name5"));
        processors.add(TestUtils.createProcessorEntity("6","name6"));
        processors.add(TestUtils.createProcessorEntity("7","name7"));
        responseRoot.getProcessGroupFlow().getFlow().setProcessors(processors);
        List<Set<?>> result = processGroupService.reorder(responseRoot.getProcessGroupFlow());
        assertEquals(2, result.get(0).size());
        assertEquals(6, result.get(1).size());
        assertEquals(5, result.get(2).size());
        assertEquals("3", ((ProcessorEntity)result.get(0).toArray()[0]).getId());
        assertEquals("1", ((ProcessorEntity)result.get(0).toArray()[1]).getId());
        assertEquals("5", ((ProcessorEntity)result.get(2).toArray()[0]).getId());
        assertEquals("7", ((ProcessorEntity)result.get(2).toArray()[1]).getId());
    }

    @Test
//    1 - 2
//    2 - 7
//    3 - 4
//    4 - 5
//    4 - 6
//    6 - 7
//    5 - 4
//    1,3 - 2,4,5,6,7
    public void reorderTestCycle() throws ApiException, IOException, URISyntaxException {
        ProcessGroupFlowEntity responseRoot = TestUtils.createProcessGroupFlowEntity("root", "rootName");
        List<ConnectionEntity> connections = new ArrayList<>();
        connections.add(TestUtils.createConnectionEntity("idCnx1", "1","2"));
        connections.add(TestUtils.createConnectionEntity("idCnx2", "2","7"));
        connections.add(TestUtils.createConnectionEntity("idCnx3", "3","4"));
        connections.add(TestUtils.createConnectionEntity("idCnx4", "4","5"));
        connections.add(TestUtils.createConnectionEntity("idCnx5", "4","6"));
        connections.add(TestUtils.createConnectionEntity("idCnx6", "6","7"));
        connections.add(TestUtils.createConnectionEntity("idCnx7", "5","4"));
        responseRoot.getProcessGroupFlow().getFlow().setConnections(connections);
        List<ProcessorEntity> processors = new ArrayList<>();
        processors = new ArrayList<>();
        processors.add(TestUtils.createProcessorEntity("1","name1"));
        processors.add(TestUtils.createProcessorEntity("2","name2"));
        processors.add(TestUtils.createProcessorEntity("3","name3"));
        processors.add(TestUtils.createProcessorEntity("4","name4"));
        processors.add(TestUtils.createProcessorEntity("5","name5"));
        processors.add(TestUtils.createProcessorEntity("6","name6"));
        processors.add(TestUtils.createProcessorEntity("7","name7"));
        responseRoot.getProcessGroupFlow().getFlow().setProcessors(processors);
        List<Set<?>> result = processGroupService.reorder(responseRoot.getProcessGroupFlow());
        assertEquals(2, result.get(0).size());
        assertEquals(7, result.get(1).size());
        assertEquals(5, result.get(2).size());
        assertEquals("3", ((ProcessorEntity)result.get(0).toArray()[0]).getId());
        assertEquals("1", ((ProcessorEntity)result.get(0).toArray()[1]).getId());
        assertEquals("5", ((ProcessorEntity)result.get(2).toArray()[0]).getId());
        assertEquals("7", ((ProcessorEntity)result.get(2).toArray()[1]).getId());
    }


    @Test
    public void getNextPositionTest() throws ApiException, IOException, URISyntaxException {
        ProcessGroupFlowEntity responseRoot = TestUtils.createProcessGroupFlowEntity("root", "root");
        responseRoot.getProcessGroupFlow().getFlow().setProcessGroups(new ArrayList<>());
        responseRoot.getProcessGroupFlow().getFlow().setProcessors(new ArrayList<>());
        responseRoot.getProcessGroupFlow().getFlow()
                .getProcessGroups().add(TestUtils.createProcessGroupEntity("idElt1", "elt1"));
        responseRoot.getProcessGroupFlow().getFlow()
                .getProcessGroups().add(TestUtils.createProcessGroupEntity("idElt2", "elt2"));
        responseRoot.getProcessGroupFlow().getFlow()
                .getProcessGroups().add(TestUtils.createProcessGroupEntity("idElt3", "elt3"));
        responseRoot.getProcessGroupFlow().getFlow()
                .getProcessors().add(TestUtils.createProcessorEntity("idProc","nameProc") );

        double x = 0;
        double y = 0;
        for (ProcessorEntity processor : responseRoot.getProcessGroupFlow().getFlow().getProcessors()) {
            PositionDTO position = new PositionDTO();
            position.setX(x);
            position.setY(y);
            processor.setPosition(position);
        }
        for (ProcessGroupEntity processGroup : responseRoot.getProcessGroupFlow().getFlow().getProcessGroups()) {
            x += 400;
            PositionDTO position = new PositionDTO();
            position.setX(x);
            position.setY(y);
            processGroup.setPosition(position);
        }
        Injector injector = Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(ProcessGroupService.class).toInstance(processGroupService);
                bind(ProcessGroupsApi.class).toInstance(processGroupsApiMock);
                bind(ProcessorService.class).toInstance(processorServiceMock);
                bind(ConnectionService.class).toInstance(connectionServiceMock);
                bind(FlowApi.class).toInstance(flowApiMock);
                bind(Integer.class).annotatedWith(Names.named("timeout")).toInstance(1);
                bind(Integer.class).annotatedWith(Names.named("interval")).toInstance(1);
                bind(Boolean.class).annotatedWith(Names.named("forceMode")).toInstance(false);
                bind(Double.class).annotatedWith(Names.named("placeWidth")).toInstance(1200d);
                bind(PositionDTO.class).annotatedWith(Names.named("startPosition")).toInstance(Main.createPosition("0,0"));
            }
        });
        PositionDTO result = injector.getInstance(ProcessGroupService.class).getNextPosition(responseRoot);
        assertEquals(0d, result.getX(), 0);
        assertEquals(220d, result.getY(), 0);
    }


}