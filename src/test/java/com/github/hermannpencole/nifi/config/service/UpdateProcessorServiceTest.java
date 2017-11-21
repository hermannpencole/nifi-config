package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.model.ConfigException;
import com.github.hermannpencole.nifi.config.model.ConnectionPort;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.FlowApi;
import com.github.hermannpencole.nifi.swagger.client.ProcessorsApi;
import com.github.hermannpencole.nifi.swagger.client.model.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * API tests for AccessApi
 */
@RunWith(MockitoJUnitRunner.class)
public class UpdateProcessorServiceTest {
    @Mock
    private ProcessGroupService processGroupServiceMock;

    @Mock
    private FlowApi flowapiMock;

    @Mock
    private ProcessorsApi processorsApiMock;

    @Mock
    private ControllerServicesService controllerServicesServiceMock;

    @Mock
    private CreateRouteService createRouteServiceMock;

    @InjectMocks
    private UpdateProcessorService updateProcessorService;

    @Test(expected = FileNotFoundException.class)
    public void updateFileNotExitingBranchTest() throws ApiException, IOException, URISyntaxException {
        List<String> branch = Arrays.asList("root", "elt1");
        updateProcessorService.updateByBranch(branch, "not existing", false);
    }

    @Test
    public void updateBranchTest() throws ApiException, IOException, URISyntaxException {
        List<String> branch = Arrays.asList("root", "elt1");
        ProcessGroupFlowEntity response = TestUtils.createProcessGroupFlowEntity("idComponent", "nameComponent");
        response.getProcessGroupFlow().getFlow()
                .getProcessors().add(TestUtils.createProcessorEntity("idProc", "nameProc"));
        response.getProcessGroupFlow().getFlow()
                .getProcessGroups().add(TestUtils.createProcessGroupEntity("idSubGroup", "nameSubGroup"));

        when(processGroupServiceMock.changeDirectory(branch)).thenReturn(Optional.of(response));
        when(flowapiMock.getFlow(response.getProcessGroupFlow().getId())).thenReturn(response);

        ProcessGroupFlowEntity subGroupResponse = TestUtils.createProcessGroupFlowEntity("idSubGroup", "nameSubGroup");
        subGroupResponse.getProcessGroupFlow().getFlow()
                .getProcessors().add(TestUtils.createProcessorEntity("idProc2", "nameProc2"));
        when(flowapiMock.getFlow(subGroupResponse.getProcessGroupFlow().getId())).thenReturn(subGroupResponse);

        updateProcessorService.updateByBranch(branch, getClass().getClassLoader().getResource("mytest1.json").getPath(), false);

        verify(processorsApiMock, times(2)).updateProcessor(any(), any());
        verify(processorsApiMock).updateProcessor(eq("idProc"), any());
        verify(processorsApiMock).updateProcessor(eq("idProc2"), any());
    }

    @Test
    public void updateBranchWithAutoTerminateRelationshipTest() throws ApiException, IOException, URISyntaxException {
        List<String> branch = Arrays.asList("root", "elt1");
        ProcessGroupFlowEntity response = TestUtils.createProcessGroupFlowEntity("idComponent", "nameComponent");
        ProcessorEntity proc = TestUtils.createProcessorEntity("idProc", "nameProc");
        RelationshipDTO relationship = new RelationshipDTO();
        relationship.setAutoTerminate(true);
        relationship.setName("testRelation");
        proc.getComponent().getRelationships().add(relationship);
        response.getProcessGroupFlow().getFlow().getProcessors().add(proc);

        when(processGroupServiceMock.changeDirectory(branch)).thenReturn(Optional.of(response));
        when(flowapiMock.getFlow(response.getProcessGroupFlow().getId())).thenReturn(response);

        updateProcessorService.updateByBranch(branch, getClass().getClassLoader().getResource("mytestAutoTerminateRelationShip.json").getPath(), false);

        verify(processorsApiMock, times(1)).updateProcessor(any(), any());
        ArgumentCaptor<ProcessorEntity> processorEntity = ArgumentCaptor.forClass(ProcessorEntity.class);
        verify(processorsApiMock).updateProcessor(eq("idProc"), processorEntity.capture());
        assertEquals(1, processorEntity.getValue().getComponent().getConfig().getAutoTerminatedRelationships().size());
        assertEquals("testRelation", processorEntity.getValue().getComponent().getConfig().getAutoTerminatedRelationships().get(0));
    }

    @Test
    public void updateBranchControllershipTest() throws ApiException, IOException, URISyntaxException, InterruptedException {
        List<String> branch = Arrays.asList("root", "elt1");
        ProcessGroupFlowEntity response = TestUtils.createProcessGroupFlowEntity("idComponent", "nameComponent");

        when(processGroupServiceMock.changeDirectory(branch)).thenReturn(Optional.of(response));
        when(flowapiMock.getFlow(response.getProcessGroupFlow().getId())).thenReturn(response);
        ControllerServicesEntity controllerServicesEntity = new ControllerServicesEntity();
        controllerServicesEntity.getControllerServices().add(TestUtils.createControllerServiceEntity("idCtrl", "nameCtrl"));
        when(flowapiMock.getControllerServicesFromGroup("idComponent")).thenReturn(controllerServicesEntity);

        updateProcessorService.updateByBranch(branch, getClass().getClassLoader().getResource("mytestController.json").getPath(), false);

        ArgumentCaptor<ControllerServiceEntity> controllerServiceEntity = ArgumentCaptor.forClass(ControllerServiceEntity.class);
        ArgumentCaptor<ControllerServiceDTO> controllerServiceDTO = ArgumentCaptor.forClass(ControllerServiceDTO.class);
        verify(controllerServicesServiceMock).updateControllerService(controllerServiceDTO.capture(), controllerServiceEntity.capture());
        assertEquals("idCtrl", controllerServiceEntity.getValue().getComponent().getId());
        assertEquals(2, controllerServiceDTO.getValue().getProperties().size());
    }

    @Test
    public void updateBranchConnectionTest() throws ApiException, IOException, URISyntaxException, InterruptedException {
        List<String> branch = Arrays.asList("root", "elt1");
        ProcessGroupFlowEntity response = TestUtils.createProcessGroupFlowEntity("idComponent", "nameComponent");

        when(processGroupServiceMock.changeDirectory(branch)).thenReturn(Optional.of(response));
        when(flowapiMock.getFlow(response.getProcessGroupFlow().getId())).thenReturn(response);

        updateProcessorService.updateByBranch(branch, getClass().getClassLoader().getResource("mytestConnection.json").getPath(), false);

        ArgumentCaptor<List<ConnectionPort>> listConnectionPorts = ArgumentCaptor.forClass((Class) List.class);
        verify(createRouteServiceMock).createRoutes(listConnectionPorts.capture(), Matchers.eq(false));
        assertEquals(1, listConnectionPorts.getValue().size());
    }

    @Test
    public void updateBranchNoConnectionTest() throws ApiException, IOException, URISyntaxException, InterruptedException {
        List<String> branch = Arrays.asList("root", "elt1");
        ProcessGroupFlowEntity response = TestUtils.createProcessGroupFlowEntity("idComponent", "nameComponent");
        response.getProcessGroupFlow().getFlow()
                .getProcessors().add(TestUtils.createProcessorEntity("idProc", "nameProc"));
        response.getProcessGroupFlow().getFlow()
                .getProcessGroups().add(TestUtils.createProcessGroupEntity("idSubGroup", "nameSubGroup"));

        when(processGroupServiceMock.changeDirectory(branch)).thenReturn(Optional.of(response));
        when(flowapiMock.getFlow(response.getProcessGroupFlow().getId())).thenReturn(response);

        ProcessGroupFlowEntity subGroupResponse = TestUtils.createProcessGroupFlowEntity("idSubGroup", "nameSubGroup");
        subGroupResponse.getProcessGroupFlow().getFlow()
                .getProcessors().add(TestUtils.createProcessorEntity("idProc2", "nameProc2"));
        when(flowapiMock.getFlow(subGroupResponse.getProcessGroupFlow().getId())).thenReturn(subGroupResponse);

        updateProcessorService.updateByBranch(branch, getClass().getClassLoader().getResource("mytest1.json").getPath(), false);

        ArgumentCaptor<List<ConnectionPort>> listConnectionPorts = ArgumentCaptor.forClass((Class) List.class);
        verify(createRouteServiceMock).createRoutes(listConnectionPorts.capture(), Matchers.eq(false));
        assertEquals(0, listConnectionPorts.getValue().size());
    }

    @Test(expected = ConfigException.class)
    public void updateErrorBranchTest() throws ApiException, IOException, URISyntaxException {
        List<String> branch = Arrays.asList("root", "elt1");
        ProcessGroupFlowEntity response = TestUtils.createProcessGroupFlowEntity("idComponent", "nameComponent");
        response.getProcessGroupFlow().getFlow()
                .getProcessors().add(TestUtils.createProcessorEntity("idProc", "nameProc"));
        response.getProcessGroupFlow().getFlow()
                .getProcessGroups().add(TestUtils.createProcessGroupEntity("idSubGroup", "nameSubGroup"));

        when(processGroupServiceMock.changeDirectory(branch)).thenReturn(Optional.of(response));
        when(flowapiMock.getFlow(response.getProcessGroupFlow().getId())).thenReturn(response);

        when(processorsApiMock.updateProcessor(any(), any())).thenThrow(new ApiException());
        updateProcessorService.updateByBranch(branch, getClass().getClassLoader().getResource("mytest1.json").getPath(), false);

    }


}