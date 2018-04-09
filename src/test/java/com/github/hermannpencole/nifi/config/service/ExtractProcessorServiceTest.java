package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.model.ConfigException;
import com.github.hermannpencole.nifi.config.model.Connection;
import com.github.hermannpencole.nifi.config.model.GroupProcessorsEntity;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.FlowApi;
import com.github.hermannpencole.nifi.swagger.client.ProcessGroupsApi;
import com.github.hermannpencole.nifi.swagger.client.model.*;
import com.google.gson.Gson;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.*;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.github.hermannpencole.nifi.config.service.TestUtils.*;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
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

    @Mock
    private ProcessGroupsApi processGroupsApiMock;

    @InjectMocks
    private ExtractProcessorService extractService;
    private List<String> branch = asList("root", "elt1");
    private File temp;
    private ProcessGroupFlowEntity response;

    @Before
    public void setup() throws IOException {
        temp = File.createTempFile("tempfile", ".tmp");
        response = TestUtils.createProcessGroupFlowEntity("idComponent", "nameComponent");

        when(processGroupsApiMock.getConnections(anyString())).thenReturn(new ConnectionsEntity());
        when(flowapiMock.getControllerServicesFromGroup("idComponent")).thenReturn(new ControllerServicesEntity());
        when(processGroupServiceMock.changeDirectory(branch)).thenReturn(Optional.of(response));
    }

    @After
    public void cleanup() {
        temp.delete();
    }

    @Test(expected = ConfigException.class)
    public void extractNotExitingBranchTest() throws ApiException, IOException {
        when(processGroupServiceMock.changeDirectory(branch)).thenReturn(Optional.empty());
        extractService.extractByBranch(branch, temp.getAbsolutePath(), false);
    }

    @Test(expected = FileNotFoundException.class)
    public void extractErrorFileBranchTest() throws ApiException, IOException {
        extractService.extractByBranch(branch, temp.getParent(), false);
    }

    @Test
    public void extractEmptyBranchTest() throws ApiException, IOException {
        extractService.extractByBranch(branch, temp.getAbsolutePath(), false);

        //evaluate response
        GroupProcessorsEntity result = loadOutputFileContent();
        assertTrue(result.getProcessors().isEmpty());
        assertTrue(result.getGroupProcessorsEntity().isEmpty());
        assertEquals("nameComponent", result.getName());
    }

    @Test
    public void extractBranchTest() throws ApiException, IOException {
        processGroupFlowEntityHas(createProcessorEntity("idProc", "nameProc"));
        response.getProcessGroupFlow().getFlow()
                .getProcessGroups().add(createProcessGroupEntity("idSubGroup", "nameSubGroup"));

        ControllerServicesEntity controllerServicesEntity = new ControllerServicesEntity();
        controllerServicesEntity.getControllerServices().add(TestUtils.createControllerServiceEntity("idCtrl", "nameCtrl"));
        when(flowapiMock.getControllerServicesFromGroup("idComponent")).thenReturn(controllerServicesEntity);

        ProcessGroupFlowEntity subGroupResponse = TestUtils.createProcessGroupFlowEntity("idSubGroup", "nameSubGroup");
        when(flowapiMock.getFlow(subGroupResponse.getProcessGroupFlow().getId())).thenReturn(subGroupResponse);

        extractService.extractByBranch(branch, temp.getAbsolutePath(), false);

        GroupProcessorsEntity result = loadOutputFileContent();

        assertEquals(1, result.getProcessors().size());
        assertEquals("nameProc", result.getProcessors().get(0).getName());
        assertEquals(1, result.getGroupProcessorsEntity().size());
        assertEquals("nameSubGroup", result.getGroupProcessorsEntity().get(0).getName());
        assertEquals("nameComponent", result.getName());
        assertEquals(1, result.getControllerServicesDTO().size());
        assertEquals("nameCtrl", result.getControllerServicesDTO().get(0).getName());
    }

    @Test(expected = ConfigException.class)
    public void extractDuplicateProcessorNamesTest() throws ApiException, IOException {
        processGroupFlowEntityHas(createProcessorEntity("idProc1", "nameProcA"));
        processGroupFlowEntityHas(createProcessorEntity("idProc2", "nameProcA"));
        processGroupFlowEntityHas(createProcessorEntity("idProc2", "nameProcB"));

        extractService.extractByBranch(branch, temp.getAbsolutePath(), true);
    }

    @Test
    public void extractNonDuplicateProcessorNamesTest() throws ApiException, IOException {
        processGroupFlowEntityHas(createProcessorEntity("idProc1", "nameProcA"));
        processGroupFlowEntityHas(createProcessorEntity("idProc2", "nameProcB"));

        extractService.extractByBranch(branch, temp.getAbsolutePath(), true);
    }

    @Test
    public void extractConnectionsTest() throws ApiException, IOException {
        processGroupFlowEntityHas(createConnectionEntity("connectionOne", "sourceOne", "destOne", "1 GB", 10L));
        processGroupFlowEntityHas(createConnectionEntity(null, "sourceTwo", "destTwo", "2 GB", 10L));
        processGroupFlowEntityHas(createConnectionEntity("connectionThree", "sourceTwo", "destOne", "1 GB", 1L));

        extractService.extractByBranch(branch, temp.getAbsolutePath(), true);

        GroupProcessorsEntity result = loadOutputFileContent();
        assertEquals(result.getConnections().size(), 3);

        assertEquals(asList("connectionOne", null, "connectionThree"), collect(result, connection -> connection.getName()));
        assertEquals(asList("sourceOne", "sourceTwo", "sourceTwo"), collect(result, connection -> connection.getSource()));
        assertEquals(asList("destOne", "destTwo", "destOne"), collect(result, connection -> connection.getDestination()));
        assertEquals(asList(10L, 10L, 1L), collect(result, connection -> connection.getBackPressureObjectThreshold()));
        assertEquals(asList("1 GB", "2 GB", "1 GB"), collect(result, connection -> connection.getBackPressureDataSizeThreshold()));
    }

    private <T> List<T> collect(GroupProcessorsEntity result, Function<Connection, T> mapper) {
        return result.getConnections()
                .stream()
                .map(mapper)
                .collect(Collectors.toList());
    }

    private void processGroupFlowEntityHas(ProcessorEntity entity) {
        response.getProcessGroupFlow().getFlow().getProcessors().add(entity);
    }

    private void processGroupFlowEntityHas(ConnectionEntity entity) {
        response.getProcessGroupFlow().getFlow().getConnections().add(entity);
    }

    private GroupProcessorsEntity loadOutputFileContent() throws IOException {
        try (Reader reader = new InputStreamReader(new FileInputStream(temp), "UTF-8")) {
            return new Gson().fromJson(reader, GroupProcessorsEntity.class);
        }
    }
}