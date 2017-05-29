package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.model.ConfigException;
import com.github.hermannpencole.nifi.config.model.GroupProcessorsEntity;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.FlowApi;
import com.github.hermannpencole.nifi.swagger.client.model.*;
import com.google.gson.Gson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

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

        ProcessGroupFlowEntity response = TestUtils.createProcessGroupFlowEntity("idComponent", "nameComponent");

        when(processGroupServiceMock.changeDirectory(branch)).thenReturn(Optional.of(response));

        when(flowapiMock.getFlow(response.getProcessGroupFlow().getId())).thenReturn(response);
        extractService.extractByBranch(branch, temp.getAbsolutePath());

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
    public void extractBranchTest() throws ApiException, IOException, URISyntaxException {
        List<String> branch = Arrays.asList("root", "elt1");
        File temp = File.createTempFile("tempfile", ".tmp");

        ProcessGroupFlowEntity response = TestUtils.createProcessGroupFlowEntity("idComponent", "nameComponent");
        response.getProcessGroupFlow().getFlow()
                .getProcessors().add(TestUtils.createProcessorEntity("idProc","nameProc") );
        response.getProcessGroupFlow().getFlow()
                .getProcessGroups().add(TestUtils.createProcessGroupEntity("idSubGroup", "nameSubGroup"));

        when(processGroupServiceMock.changeDirectory(branch)).thenReturn(Optional.of(response));
        when(flowapiMock.getFlow(response.getProcessGroupFlow().getId())).thenReturn(response);

        ProcessGroupFlowEntity subGroupResponse = TestUtils.createProcessGroupFlowEntity("idSubGroup", "nameSubGroup");
        when(flowapiMock.getFlow(subGroupResponse.getProcessGroupFlow().getId())).thenReturn(subGroupResponse);

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