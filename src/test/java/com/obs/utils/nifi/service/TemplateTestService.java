package com.obs.utils.nifi.service;

import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.ProcessGroupsApi;
import com.github.hermannpencole.nifi.swagger.client.model.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

import static org.mockito.Mockito.*;

/**
 * API tests for AccessApi
 */
@RunWith(MockitoJUnitRunner.class)
public class TemplateTestService {
    @Mock
    private ProcessGroupService processGroupServiceMock;
    @Mock
    private ProcessGroupsApi processGroupsApiMock;
    @InjectMocks
    private TemplateService templateService;

    /**
     * Creates a token for accessing the REST API via username/password
     *
     * The token returned is formatted as a JSON Web Token (JWT). The token is base64 encoded and comprised of three parts. The header, the body, and the signature. The expiration of the token is a contained within the body. The token can be used in the Authorization header in the format &#39;Authorization: Bearer &lt;token&gt;&#39;.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createAccessTokenTest() throws ApiException, IOException, URISyntaxException {
        List<String> branch = Arrays.asList("root","elt1");
        String fileName= "test";
        ProcessGroupFlowDTO processGroupFlow = new ProcessGroupFlowDTO();
        processGroupFlow.setId("idProcessGroupFlow");
        when(processGroupServiceMock.createDirectory(branch)).thenReturn(processGroupFlow);
        TemplateEntity template = new TemplateEntity();
        template.setId("idTemplate");
        when(processGroupsApiMock.uploadTemplate(anyString(), any())).thenReturn(template);
        //when(processGroupsApiMock.uploadTemplate(processGroupFlow.getId(), new File(fileName))).thenReturn(template);

        templateService.installOnBranch(branch, fileName);

        InstantiateTemplateRequestEntity instantiateTemplate = new InstantiateTemplateRequestEntity(); // InstantiateTemplateRequestEntity | The instantiate template request.
        instantiateTemplate.setTemplateId(template.getId());
        instantiateTemplate.setOriginX(0d);
        instantiateTemplate.setOriginY(0d);

        verify(processGroupServiceMock).createDirectory(branch);
        verify(processGroupsApiMock).uploadTemplate(processGroupFlow.getId(),new File(fileName));
        verify(processGroupsApiMock).instantiateTemplate(processGroupFlow.getId(), instantiateTemplate);
    }
    

    
}
