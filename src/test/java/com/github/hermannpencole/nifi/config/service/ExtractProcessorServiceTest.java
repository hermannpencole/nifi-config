package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.model.ConfigException;
import com.github.hermannpencole.nifi.config.model.GroupProcessorsEntity;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.FlowApi;
import com.github.hermannpencole.nifi.swagger.client.model.ControllerServicesEntity;
import com.github.hermannpencole.nifi.swagger.client.model.ProcessGroupFlowEntity;
import com.github.hermannpencole.nifi.swagger.client.model.ProcessorDTO;
import com.google.gson.Gson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.github.hermannpencole.nifi.swagger.client.model.ProcessorDTO.StateEnum.RUNNING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * API tests for AccessApi
 */
@RunWith(MockitoJUnitRunner.class)
public class ExtractProcessorServiceTest {
    @Mock
    private ProcessGroupService processGroupServiceMock;

    @Mock
    private FlowApi flowapiMock;

    @InjectMocks
    private ExtractProcessorService extractService;

    @Test(expected = ConfigException.class)
    public void extractNotExitingBranchTest() throws ApiException, IOException {
        List<String> branch = Arrays.asList("root", "elt1");
        File temp = File.createTempFile("tempfile", ".tmp");
        when(processGroupServiceMock.changeDirectory(branch)).thenReturn(Optional.empty());
        extractService.extractByBranch(branch, temp.getAbsolutePath(), false);
    }

    @Test(expected = FileNotFoundException.class)
    public void extractErrorFileBranchTest() throws ApiException, IOException {
        List<String> branch = Arrays.asList("root", "elt1");
        ProcessGroupFlowEntity response = TestUtils.createProcessGroupFlowEntity("idComponent", "nameComponent");
        when(processGroupServiceMock.changeDirectory(branch)).thenReturn(Optional.of(response));
        when(flowapiMock.getControllerServicesFromGroup("idComponent")).thenReturn(new ControllerServicesEntity());

        File temp = File.createTempFile("tempfile", ".tmp");
        extractService.extractByBranch(branch, temp.getParent(), false);
    }

    @Test
    public void extractEmptyBranchTest() throws ApiException, IOException {
        List<String> branch = Arrays.asList("root", "elt1");
        File temp = File.createTempFile("tempfile", ".tmp");

        ProcessGroupFlowEntity response = TestUtils.createProcessGroupFlowEntity("idComponent", "nameComponent");

        when(processGroupServiceMock.changeDirectory(branch)).thenReturn(Optional.of(response));
        when(flowapiMock.getControllerServicesFromGroup("idComponent")).thenReturn(new ControllerServicesEntity());

        extractService.extractByBranch(branch, temp.getAbsolutePath(), false);

        //evaluate response
        Gson gson = new Gson();
        try (Reader reader = new InputStreamReader(new FileInputStream(temp), "UTF-8")) {
            GroupProcessorsEntity result = gson.fromJson(reader, GroupProcessorsEntity.class);
            assertTrue(result.getProcessors().isEmpty());
            assertTrue(result.getGroupProcessorsEntity().isEmpty());
            assertEquals("nameComponent", result.getName());
        }
    }

    @Test
    public void extractBranchTest() throws ApiException, IOException {
        List<String> branch = Arrays.asList("root", "elt1");
        File temp = File.createTempFile("tempfile", ".tmp");

        ProcessGroupFlowEntity response = TestUtils.createProcessGroupFlowEntity("idComponent", "nameComponent");
        response.getProcessGroupFlow().getFlow()
                .getProcessors().add(TestUtils.createProcessorEntity("idProc", "nameProc", RUNNING));
        response.getProcessGroupFlow().getFlow()
                .getProcessGroups().add(TestUtils.createProcessGroupEntity("idSubGroup", "nameSubGroup"));

        when(processGroupServiceMock.changeDirectory(branch)).thenReturn(Optional.of(response));
        ControllerServicesEntity controllerServicesEntity = new ControllerServicesEntity();
        controllerServicesEntity.getControllerServices().add(TestUtils.createControllerServiceEntity("idCtrl", "nameCtrl"));
        when(flowapiMock.getControllerServicesFromGroup("idComponent")).thenReturn(controllerServicesEntity);

        ProcessGroupFlowEntity subGroupResponse = TestUtils.createProcessGroupFlowEntity("idSubGroup", "nameSubGroup");
        when(flowapiMock.getFlow(subGroupResponse.getProcessGroupFlow().getId())).thenReturn(subGroupResponse);

        extractService.extractByBranch(branch, temp.getAbsolutePath(), false);
        Gson gson = new Gson();
        try (Reader reader = new InputStreamReader(new FileInputStream(temp), "UTF-8")) {
            GroupProcessorsEntity result = gson.fromJson(reader, GroupProcessorsEntity.class);
            assertEquals(1, result.getProcessors().size());

            ProcessorDTO processorDTO = result.getProcessors().get(0);
            assertEquals("nameProc", processorDTO.getName());
            assertEquals(RUNNING, processorDTO.getState());

            assertEquals(1, result.getGroupProcessorsEntity().size());
            assertEquals("nameSubGroup", result.getGroupProcessorsEntity().get(0).getName());
            assertEquals("nameComponent", result.getName());
            assertEquals(1, result.getControllerServicesDTO().size());
            assertEquals("nameCtrl", result.getControllerServicesDTO().get(0).getName());
        }
    }

    @Test(expected = ConfigException.class)
    public void extractDuplicateProcessorNamesTest() throws ApiException, IOException {
        List<String> branch = Arrays.asList("root", "elt1");
        File temp = File.createTempFile("tempfile", ".tmp");

        ProcessGroupFlowEntity response = TestUtils.createProcessGroupFlowEntity("idComponent", "nameComponent");
        response.getProcessGroupFlow().getFlow()
                .getProcessors().add(TestUtils.createProcessorEntity("idProc1", "nameProcA"));
        response.getProcessGroupFlow().getFlow()
                .getProcessors().add(TestUtils.createProcessorEntity("idProc2", "nameProcA"));
        response.getProcessGroupFlow().getFlow()
                .getProcessors().add(TestUtils.createProcessorEntity("idProc2", "nameProcB"));

        when(processGroupServiceMock.changeDirectory(branch)).thenReturn(Optional.of(response));
        when(flowapiMock.getControllerServicesFromGroup("idComponent")).thenReturn(new ControllerServicesEntity());

        extractService.extractByBranch(branch, temp.getAbsolutePath(), true);
    }

    @Test
    public void extractNonDuplicateProcessorNamesTest() throws ApiException, IOException {
        List<String> branch = Arrays.asList("root", "elt1");
        File temp = File.createTempFile("tempfile", ".tmp");

        ProcessGroupFlowEntity response = TestUtils.createProcessGroupFlowEntity("idComponent", "nameComponent");
        response.getProcessGroupFlow().getFlow()
                .getProcessors().add(TestUtils.createProcessorEntity("idProc1", "nameProcA"));
        response.getProcessGroupFlow().getFlow()
                .getProcessors().add(TestUtils.createProcessorEntity("idProc2", "nameProcB"));

        when(processGroupServiceMock.changeDirectory(branch)).thenReturn(Optional.of(response));
        when(flowapiMock.getControllerServicesFromGroup("idComponent")).thenReturn(new ControllerServicesEntity());

        extractService.extractByBranch(branch, temp.getAbsolutePath(), true);
    }
}