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
import java.util.ArrayList;
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
        when(flowapiMock.getControllerServicesFromGroup("idComponent", true, false)).thenReturn(new ControllerServicesEntity());
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
        if (response.getProcessGroupFlow().getFlow().getProcessGroups() == null) response.getProcessGroupFlow().getFlow().setProcessGroups(new ArrayList<>());
        response.getProcessGroupFlow().getFlow()
                .getProcessGroups().add(createProcessGroupEntity("idSubGroup", "nameSubGroup"));

        ControllerServicesEntity controllerServicesEntity = new ControllerServicesEntity();
        controllerServicesEntity.setControllerServices(new ArrayList<>());
        controllerServicesEntity.getControllerServices().add(TestUtils.createControllerServiceEntity("idCtrl", "nameCtrl"));
        when(flowapiMock.getControllerServicesFromGroup("idComponent", true, false)).thenReturn(controllerServicesEntity);

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
        processGroupFlowEntityHas(createConnectionEntity("connectionOneId", "connectionOne", "sourceOne", "destOne", "1 GB", 10L));
        processGroupFlowEntityHas(createConnectionEntity("connectionTwoId", null, "sourceTwo", "destTwo", "2 GB", 10L));
        processGroupFlowEntityHas(createConnectionEntity("connectionThreeId", "connectionThree", "sourceTwo", "destOne", "1 GB", 1L));

        extractService.extractByBranch(branch, temp.getAbsolutePath(), true);

        GroupProcessorsEntity result = loadOutputFileContent();
        assertEquals(result.getConnections().size(), 3);

        assertEquals(asList("connectionOne", null, "connectionThree"), mapAndCollect(result, connection -> connection.getName()));
        assertEquals(asList("sourceOne", "sourceTwo", "sourceTwo"), mapAndCollect(result, connection -> connection.getSource()));
        assertEquals(asList("destOne", "destTwo", "destOne"), mapAndCollect(result, connection -> connection.getDestination()));
        assertEquals(asList(10L, 10L, 1L), mapAndCollect(result, connection -> connection.getBackPressureObjectThreshold()));
        assertEquals(asList("1 GB", "2 GB", "1 GB"), mapAndCollect(result, connection -> connection.getBackPressureDataSizeThreshold()));
    }

    @Test
    public void extractConnectionsFromSubFlowsTest() throws ApiException, IOException {
        processGroupFlowEntityHas(createConnectionEntity("connectionOneId", "connectionOne", "sourceOne", "destOne", "1 GB", 10L));
        processGroupFlowEntityHas(createProcessGroupEntity("subGroupId", "sub group"));

        ProcessGroupFlowEntity subGroupEntity = createProcessGroupFlowEntity("subComponent", "subComponent");
        when(flowapiMock.getFlow("subGroupId")).thenReturn(subGroupEntity);
        processGroupFlowEntityHas(subGroupEntity, createConnectionEntity("subConnectionId", "subConnection", "sourceOne", "destOne", "2 GB", 12L));

        extractService.extractByBranch(branch, temp.getAbsolutePath(), true);

        GroupProcessorsEntity result = loadOutputFileContent();
        assertEquals(result.getConnections().size(), 1);

        Connection subGroupConnection = result.getGroupProcessorsEntity().get(0).getConnections().get(0);

        assertEquals("subConnectionId", subGroupConnection.getId());
        assertEquals("subConnection", subGroupConnection.getName());
        assertEquals("sourceOne", subGroupConnection.getSource());
        assertEquals("destOne", subGroupConnection.getDestination());
        assertEquals(12L, subGroupConnection.getBackPressureObjectThreshold().longValue());
        assertEquals("2 GB", subGroupConnection.getBackPressureDataSizeThreshold());
    }

    private <T> List<T> mapAndCollect(GroupProcessorsEntity result, Function<Connection, T> mapper) {
        return result.getConnections()
                .stream()
                .map(mapper)
                .collect(Collectors.toList());
    }

    private void processGroupFlowEntityHas(ProcessorEntity entity) {
        if ( response.getProcessGroupFlow().getFlow().getProcessors() == null)  response.getProcessGroupFlow().getFlow().setProcessors(new ArrayList<>());
        response.getProcessGroupFlow().getFlow().getProcessors().add(entity);
    }

    private void processGroupFlowEntityHas(ConnectionEntity entity) {
        if ( response.getProcessGroupFlow().getFlow().getConnections() == null)  response.getProcessGroupFlow().getFlow().setConnections(new ArrayList<>());
        response.getProcessGroupFlow().getFlow().getConnections().add(entity);
    }

    private void processGroupFlowEntityHas(ProcessGroupEntity processGroupEntity) {
        if ( response.getProcessGroupFlow().getFlow().getProcessGroups() == null)  response.getProcessGroupFlow().getFlow().setProcessGroups(new ArrayList<>());
        response.getProcessGroupFlow().getFlow().getProcessGroups().add(processGroupEntity);
    }

    private void processGroupFlowEntityHas(ProcessGroupFlowEntity groupEntity, ConnectionEntity connectionEntity) {
        if ( groupEntity.getProcessGroupFlow().getFlow().getConnections() == null)  groupEntity.getProcessGroupFlow().getFlow().setConnections(new ArrayList<>());
        groupEntity.getProcessGroupFlow().getFlow().getConnections().add(connectionEntity);
    }

    private GroupProcessorsEntity loadOutputFileContent() throws IOException {
        try (Reader reader = new InputStreamReader(new FileInputStream(temp), "UTF-8")) {
            return new Gson().fromJson(reader, GroupProcessorsEntity.class);
        }
    }
}