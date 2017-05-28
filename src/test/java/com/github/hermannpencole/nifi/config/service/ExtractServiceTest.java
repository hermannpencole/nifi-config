package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.model.ConfigException;
import com.github.hermannpencole.nifi.config.model.GroupProcessorsEntity;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.AccessApi;
import com.github.hermannpencole.nifi.swagger.client.FlowApi;
import com.github.hermannpencole.nifi.swagger.client.model.*;
import com.google.gson.Gson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.inject.Inject;
import java.io.*;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
/**
 * API tests for AccessApi
 */
@RunWith(MockitoJUnitRunner.class)
public class ExtractServiceTest {
    @Mock
    private ProcessGroupService processGroupServiceMock;

    @Mock
    private FlowApi flowapiMock;

    @InjectMocks
    private ExtractProcessorService extractService;

    /**
     * Creates a token for accessing the REST API via username/password
     * <p>
     * The token returned is formatted as a JSON Web Token (JWT). The token is base64 encoded and comprised of three parts. The header, the body, and the signature. The expiration of the token is a contained within the body. The token can be used in the Authorization header in the format &#39;Authorization: Bearer &lt;token&gt;&#39;.
     *
     * @throws ApiException if the Api call fails
     */
    @Test(expected = ConfigException.class)
    public void extractNotExitingBranchTest() throws ApiException, IOException, URISyntaxException {
        List<String> branch = Arrays.asList("root", "elt1");
        File temp = File.createTempFile("tempfile", ".tmp");
        when(processGroupServiceMock.changeDirectory(branch)).thenReturn(Optional.empty());
        extractService.extractByBranch(branch, temp.getAbsolutePath());
    }

    @Test
    public void extractEmptyBranchTest() throws ApiException, IOException, URISyntaxException {
        List<String> branch = Arrays.asList("root", "elt1");
        File temp = File.createTempFile("tempfile", ".tmp");
        ProcessGroupFlowDTO componentSearch = new ProcessGroupFlowDTO();
        componentSearch.setId("idComponent");
        componentSearch.setBreadcrumb(new FlowBreadcrumbEntity());
        componentSearch.getBreadcrumb().setBreadcrumb(new FlowBreadcrumbDTO());
        componentSearch.getBreadcrumb().getBreadcrumb().setName("nameComponent");

        when(processGroupServiceMock.changeDirectory(branch)).thenReturn(Optional.of(componentSearch));
        FlowDTO flow = new FlowDTO();
        ProcessGroupFlowEntity response = new ProcessGroupFlowEntity();
        response.setProcessGroupFlow(componentSearch);
        response.getProcessGroupFlow().setFlow(flow);

        when(flowapiMock.getFlow(componentSearch.getId())).thenReturn(response);
        extractService.extractByBranch(branch, temp.getAbsolutePath());
        Gson gson = new Gson();
        try (Reader reader = new InputStreamReader(new FileInputStream(temp), "UTF-8")) {
            GroupProcessorsEntity result = gson.fromJson(reader, GroupProcessorsEntity.class);
            assertTrue(result.getProcessors().isEmpty());
            assertTrue(result.getGroupProcessorsEntity().isEmpty());
            assertEquals("nameComponent", result.getName());
        }


    }

    @Test
    public void extractBranchTest() throws ApiException, IOException, URISyntaxException {
        List<String> branch = Arrays.asList("root", "elt1");
        File temp = File.createTempFile("tempfile", ".tmp");
        ProcessGroupFlowDTO componentSearch = new ProcessGroupFlowDTO();
        componentSearch.setId("idComponent");
        componentSearch.setBreadcrumb(new FlowBreadcrumbEntity());
        componentSearch.getBreadcrumb().setBreadcrumb(new FlowBreadcrumbDTO());
        componentSearch.getBreadcrumb().getBreadcrumb().setName("nameComponent");

        when(processGroupServiceMock.changeDirectory(branch)).thenReturn(Optional.of(componentSearch));
        FlowDTO flow = new FlowDTO();

        ProcessGroupFlowEntity response = new ProcessGroupFlowEntity();
        response.setProcessGroupFlow(componentSearch);
        response.getProcessGroupFlow().setFlow(flow);
        ProcessorEntity proc = new ProcessorEntity();
        ProcessorDTO procDTO = new ProcessorDTO();
        procDTO.setName("nameProc");
        procDTO.setConfig(new ProcessorConfigDTO());
        proc.setComponent(procDTO);
        flow.getProcessors().add(proc);

        ProcessGroupEntity processGroupEntity = new ProcessGroupEntity();
        processGroupEntity.setId("idSubGroup");
        ProcessGroupDTO processGroupDTO = new ProcessGroupDTO();
        processGroupDTO.setName("nameSubGroup");
        processGroupEntity.setComponent(processGroupDTO);
        flow.getProcessGroups().add(processGroupEntity);
        when(flowapiMock.getFlow(componentSearch.getId())).thenReturn(response);

        ProcessGroupFlowDTO subGroup = new ProcessGroupFlowDTO();
        subGroup.setId("idSubGroup");
        subGroup.setBreadcrumb(new FlowBreadcrumbEntity());
        subGroup.getBreadcrumb().setBreadcrumb(new FlowBreadcrumbDTO());
        subGroup.getBreadcrumb().getBreadcrumb().setName("nameSubGroup");
        flow = new FlowDTO();
        response = new ProcessGroupFlowEntity();
        response.setProcessGroupFlow(subGroup);
        response.getProcessGroupFlow().setFlow(flow);
        when(flowapiMock.getFlow(subGroup.getId())).thenReturn(response);


        extractService.extractByBranch(branch, temp.getAbsolutePath());
        Gson gson = new Gson();
        try (Reader reader = new InputStreamReader(new FileInputStream(temp), "UTF-8")) {
            GroupProcessorsEntity result = gson.fromJson(reader, GroupProcessorsEntity.class);
            assertEquals(1, result.getProcessors().size());
            assertEquals("nameProc", result.getProcessors().get(0).getName());
            assertEquals(1,result.getGroupProcessorsEntity().size());
            assertEquals("nameSubGroup", result.getGroupProcessorsEntity().get(0).getName());
            assertEquals("nameComponent", result.getName());
        }


    }




}