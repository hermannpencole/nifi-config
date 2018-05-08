package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.model.ConfigException;
import com.github.hermannpencole.nifi.config.model.Connection;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.FlowApi;
import com.github.hermannpencole.nifi.swagger.client.ProcessorsApi;
import com.github.hermannpencole.nifi.swagger.client.model.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.github.hermannpencole.nifi.config.service.TestUtils.*;
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
    private ConnectionsUpdater connectionsUpdater;

    @Mock
    private CreateRouteService createRouteServiceMock;

    @InjectMocks
    private UpdateProcessorService updateProcessorService;

    private List<String> branch = Arrays.asList("root", "elt1");
    ProcessGroupFlowEntity response;

    @Before
    public void setup() {
        response = createProcessGroupFlowEntity("idComponent", "nameComponent");
        when(processGroupServiceMock.changeDirectory(branch)).thenReturn(Optional.of(response));
        when(flowapiMock.getFlow(response.getProcessGroupFlow().getId())).thenReturn(response);
    }

    @Test(expected = FileNotFoundException.class)
    public void updateFileNotExitingBranchTest() throws ApiException, IOException {
        updateProcessorService.updateByBranch(branch, "not existing", false);
    }

    @Test
    public void updateBranchTest() throws ApiException, IOException {

        processGroupFlowEntityHas(createProcessorEntity("idProc", "nameProc"));
        processGroupFlowEntityHas(createProcessGroupEntity("idSubGroup", "nameSubGroup"));

        ConnectionEntity connectionEntity = createConnectionEntity("idConnection", "sourceId", "destinationId");
        processGroupFlowEntityHas(connectionEntity);

        ProcessGroupFlowEntity subGroupResponse = createProcessGroupFlowEntity("idSubGroup", "nameSubGroup");
        processGroupFlowEntityHas(subGroupResponse, createProcessorEntity("idProc2", "nameProc2"));
        processGroupFlowEntityHas(subGroupResponse, createProcessorEntity("idProc3", "nameProc3"));
        ConnectionEntity subConnection = createConnectionEntity("subConnection", "sourceId", "destinationId");
        processGroupFlowEntityHas(subGroupResponse, subConnection);
        when(flowapiMock.getFlow(subGroupResponse.getProcessGroupFlow().getId())).thenReturn(subGroupResponse);

        updateProcessorService.updateByBranch(branch, resourcePath("mytest1.json").getPath(), false);

        verify(processorsApiMock, times(3)).updateProcessor(any(), any());
        verify(processorsApiMock).updateProcessor(eq("idProc"), any());
        verify(processorsApiMock).updateProcessor(eq("idProc2"), any());
        verify(processorsApiMock).updateProcessor(eq("idProc3"), any());

        Connection connectionInConfigurationFile = createConnection("idConnection", "sourceOne", "destOne", "1 GB", 10L, "idConnection");
        verify(connectionsUpdater, times(1)).updateConnections(Arrays.asList(connectionInConfigurationFile), Arrays.asList(connectionEntity));
        Connection subGroupConnection = createConnection("subGroupConnection", "nameProc2", "nameProc3", "4 GB", 4L, "idConnection");
        verify(connectionsUpdater, times(1)).updateConnections(Arrays.asList(subGroupConnection), Arrays.asList(subConnection));
    }

    @Test
    public void updateBranchWithAutoTerminateRelationshipTest() throws ApiException, IOException {
        ProcessorEntity proc = createProcessorEntity("idProc", "nameProc");

        RelationshipDTO relationship = new RelationshipDTO();
        relationship.setAutoTerminate(true);
        relationship.setName("testRelation");
        proc.getComponent().getRelationships().add(relationship);
        processGroupFlowEntityHas(proc);

        updateProcessorService.updateByBranch(branch, resourcePath("mytestAutoTerminateRelationShip.json").getPath(), false);

        verify(processorsApiMock, times(1)).updateProcessor(any(), any());
        ArgumentCaptor<ProcessorEntity> processorEntity = ArgumentCaptor.forClass(ProcessorEntity.class);
        verify(processorsApiMock).updateProcessor(eq("idProc"), processorEntity.capture());
        assertEquals(1, processorEntity.getValue().getComponent().getConfig().getAutoTerminatedRelationships().size());
        assertEquals("testRelation", processorEntity.getValue().getComponent().getConfig().getAutoTerminatedRelationships().get(0));
    }

    @Test
    public void updateBranchControllerTest() throws ApiException, IOException {
        ControllerServicesEntity controllerServicesEntity = new ControllerServicesEntity();
        controllerServicesEntity.getControllerServices().add(TestUtils.createControllerServiceEntity("idCtrl", "nameCtrl"));
        when(flowapiMock.getControllerServicesFromGroup("idComponent")).thenReturn(controllerServicesEntity);
        when(controllerServicesServiceMock.setStateControllerService(any(), any())).thenReturn(controllerServicesEntity.getControllerServices().get(0));
        when(controllerServicesServiceMock.updateControllerService(any(), any(), eq(false))).thenReturn(controllerServicesEntity.getControllerServices().get(0));

        updateProcessorService.updateByBranch(branch, resourcePath("mytestController.json").getPath(), false);

        ArgumentCaptor<ControllerServiceEntity> controllerServiceEntity = ArgumentCaptor.forClass(ControllerServiceEntity.class);
        ArgumentCaptor<ControllerServiceDTO> controllerServiceDTO = ArgumentCaptor.forClass(ControllerServiceDTO.class);
        verify(controllerServicesServiceMock).updateControllerService(controllerServiceDTO.capture(), controllerServiceEntity.capture(), eq(false));
        assertEquals("idCtrl", controllerServiceEntity.getValue().getComponent().getId());
        assertEquals(2, controllerServiceDTO.getValue().getProperties().size());
    }

    @Test(expected = ConfigException.class)
    public void updateErrorBranchTest() throws ApiException, IOException {
        processGroupFlowEntityHas(createProcessorEntity("idProc", "nameProc"));
        processGroupFlowEntityHas(createProcessGroupEntity("idSubGroup", "nameSubGroup"));

        when(processorsApiMock.updateProcessor(any(), any())).thenThrow(new ApiException());
        updateProcessorService.updateByBranch(branch, resourcePath("mytest1.json").getPath(), false);
    }

    private URL resourcePath(String resourceName) {
        return getClass().getClassLoader().getResource(resourceName);
    }

    private boolean processGroupFlowEntityHas(ProcessGroupFlowEntity response, ProcessorEntity processorEntity) {
        return response.getProcessGroupFlow().getFlow().getProcessors().add(processorEntity);
    }

    private boolean processGroupFlowEntityHas(ProcessGroupFlowEntity response, ConnectionEntity connectionEntity) {
        return response.getProcessGroupFlow().getFlow().getConnections().add(connectionEntity);
    }

    private boolean processGroupFlowEntityHas(ProcessGroupEntity groupEntity) {
        return response.getProcessGroupFlow().getFlow().getProcessGroups().add(groupEntity);
    }

    private boolean processGroupFlowEntityHas(ConnectionEntity connectionEntity) {
        return response.getProcessGroupFlow().getFlow().getConnections().add(connectionEntity);
    }

    private boolean processGroupFlowEntityHas(ProcessorEntity processorEntity) {
        return processGroupFlowEntityHas(response, processorEntity);
    }
}