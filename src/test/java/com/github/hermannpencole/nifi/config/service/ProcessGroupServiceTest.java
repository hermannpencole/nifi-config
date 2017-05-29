package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.FlowApi;
import com.github.hermannpencole.nifi.swagger.client.ProcessGroupsApi;
import com.github.hermannpencole.nifi.swagger.client.model.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;
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
        processGroupService.setState("id", ScheduleComponentsEntity.StateEnum.RUNNING);
        ScheduleComponentsEntity body = new ScheduleComponentsEntity();
        body.setId("id");
        body.setState(ScheduleComponentsEntity.StateEnum.RUNNING);
        body.setComponents(null);//for all
        verify(flowapiMock).scheduleComponents("id", body);
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