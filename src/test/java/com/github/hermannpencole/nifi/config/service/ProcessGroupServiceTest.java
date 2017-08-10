package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.FlowApi;
import com.github.hermannpencole.nifi.swagger.client.ProcessGroupsApi;
import com.github.hermannpencole.nifi.swagger.client.model.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * API tests for AccessApi
 */
@RunWith(MockitoJUnitRunner.class)
public class ProcessGroupServiceTest {
    @Mock
    private FlowApi flowapiMock;

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
        responseRoot.getProcessGroupFlow().getFlow()
                .getProcessGroups().add(TestUtils.createProcessGroupEntity("idElt1", "elt1"));
        when(flowapiMock.getFlow(responseRoot.getProcessGroupFlow().getId())).thenReturn(responseRoot);

        Optional<ProcessGroupFlowEntity> response = processGroupService.changeDirectory(branch);
        assertFalse(response.isPresent());
     }

    @Test
    public void changeDirectoryExitingBranchTest() throws ApiException, IOException, URISyntaxException {
        List<String> branch = Arrays.asList("root", "elt1");

        ProcessGroupFlowEntity responseRoot = TestUtils.createProcessGroupFlowEntity("root", "root");
        responseRoot.getProcessGroupFlow().getFlow()
                .getProcessGroups().add(TestUtils.createProcessGroupEntity("idElt1", "elt1"));
        when(flowapiMock.getFlow(responseRoot.getProcessGroupFlow().getId())).thenReturn(responseRoot);
        ProcessGroupFlowEntity responseElt = TestUtils.createProcessGroupFlowEntity("idElt1", "elt1");
        when(flowapiMock.getFlow(responseElt.getProcessGroupFlow().getId())).thenReturn(responseElt);

        Optional<ProcessGroupFlowEntity> response = processGroupService.changeDirectory(branch);
        assertTrue(response.isPresent());
        assertEquals("idElt1", response.get().getProcessGroupFlow().getId());
    }

    @Test
    public void createDirectoryNotExitingTest() throws ApiException, IOException, URISyntaxException {
        List<String> branch = Arrays.asList("root", "elt2");

        ProcessGroupFlowEntity responseRoot = TestUtils.createProcessGroupFlowEntity("root", "root");
        responseRoot.getProcessGroupFlow().getFlow()
                .getProcessGroups().add(TestUtils.createProcessGroupEntity("idElt1", "elt1"));
        when(flowapiMock.getFlow(responseRoot.getProcessGroupFlow().getId())).thenReturn(responseRoot);
        ProcessGroupFlowEntity responseElt = TestUtils.createProcessGroupFlowEntity("idElt2", "elt2");
        when(processGroupsApiMock.createProcessGroup(any(), any())).thenReturn(TestUtils.createProcessGroupEntity("idElt2", "elt2"));
        when(flowapiMock.getFlow(responseElt.getProcessGroupFlow().getId())).thenReturn(responseElt);

        ProcessGroupFlowEntity response = processGroupService.createDirectory(branch);
        assertEquals("idElt2", response.getProcessGroupFlow().getId());
    }

    @Test
    public void createDirectoryExitingBranchTest() throws ApiException, IOException, URISyntaxException {
        List<String> branch = Arrays.asList("root", "elt1");

        ProcessGroupFlowEntity responseRoot = TestUtils.createProcessGroupFlowEntity("root", "root");
        responseRoot.getProcessGroupFlow().getFlow()
                .getProcessGroups().add(TestUtils.createProcessGroupEntity("idElt1", "elt1"));
        when(flowapiMock.getFlow(responseRoot.getProcessGroupFlow().getId())).thenReturn(responseRoot);
        ProcessGroupFlowEntity responseElt = TestUtils.createProcessGroupFlowEntity("idElt1", "elt1");
        when(flowapiMock.getFlow(responseElt.getProcessGroupFlow().getId())).thenReturn(responseElt);

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
        verify(flowapiMock).scheduleComponents("root", body);
    }

    @Test
    public void startTest() throws ApiException, IOException, URISyntaxException {
        ProcessGroupFlowEntity responseRoot = TestUtils.createProcessGroupFlowEntity("root", "rootName");
        List<ConnectionEntity> connections = new ArrayList<>();
        ConnectionEntity connectionEntity = new ConnectionEntity();
        connectionEntity.setSourceId("idProc");
        connectionEntity.setDestinationId("idProc2");
        connections.add(connectionEntity);
        responseRoot.getProcessGroupFlow().getFlow().setConnections(connections);
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
        ConnectionEntity connectionEntity = new ConnectionEntity();
        connectionEntity.setSourceId("idProc");
        connectionEntity.setDestinationId("idProc2");
        connections.add(connectionEntity);
        responseRoot.getProcessGroupFlow().getFlow().setConnections(connections);
        responseRoot.getProcessGroupFlow().getFlow()
                .getProcessors().add(TestUtils.createProcessorEntity("idProc","nameProc") );
        responseRoot.getProcessGroupFlow().getFlow()
                .getProcessors().add(TestUtils.createProcessorEntity("idProc2","nameProc2") );
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
        ConnectionEntity connectionEntity = new ConnectionEntity();
        connectionEntity.setSourceId("1");
        connectionEntity.setDestinationId("2");
        connections.add(connectionEntity);
        connectionEntity = new ConnectionEntity();
        connectionEntity.setSourceId("2");
        connectionEntity.setDestinationId("7");
        connections.add(connectionEntity);
        connectionEntity = new ConnectionEntity();
        connectionEntity.setSourceId("3");
        connectionEntity.setDestinationId("4");
        connections.add(connectionEntity);
        connectionEntity = new ConnectionEntity();
        connectionEntity.setSourceId("4");
        connectionEntity.setDestinationId("5");
        connections.add(connectionEntity);
        connectionEntity = new ConnectionEntity();
        connectionEntity.setSourceId("4");
        connectionEntity.setDestinationId("6");
        connections.add(connectionEntity);
        connectionEntity = new ConnectionEntity();
        connectionEntity.setSourceId("6");
        connectionEntity.setDestinationId("7");
        connections.add(connectionEntity);
        responseRoot.getProcessGroupFlow().getFlow().setConnections(connections);
        processGroupService.reorder(responseRoot.getProcessGroupFlow().getFlow());

    }

    @Test
    public void getNextPositionTest() throws ApiException, IOException, URISyntaxException {
        ProcessGroupFlowEntity responseRoot = TestUtils.createProcessGroupFlowEntity("root", "root");
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
        PositionDTO result = processGroupService.getNextPosition(responseRoot);
        assertEquals(0d, result.getX(), 0);
        assertEquals(200d, result.getY(), 0);
    }


}