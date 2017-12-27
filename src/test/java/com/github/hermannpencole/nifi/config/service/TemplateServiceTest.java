package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.FlowApi;
import com.github.hermannpencole.nifi.swagger.client.ProcessGroupsApi;
import com.github.hermannpencole.nifi.swagger.client.TemplatesApi;
import com.github.hermannpencole.nifi.swagger.client.model.*;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

/**
 * API tests for AccessApi
 */
@RunWith(MockitoJUnitRunner.class)
public class TemplateServiceTest {
    @Mock
    private ProcessGroupService processGroupServiceMock;
    @Mock
    private ProcessGroupsApi processGroupsApiMock;
    @Mock
    private TemplatesApi templatesApiMock;
    @Mock
    private FlowApi flowApiMock;
    @InjectMocks
    private TemplateService templateService;

    /**
     * Creates a token for accessing the REST API via username/password
     * <p>
     * The token returned is formatted as a JSON Web Token (JWT). The token is base64 encoded and comprised of three parts. The header, the body, and the signature. The expiration of the token is a contained within the body. The token can be used in the Authorization header in the format &#39;Authorization: Bearer &lt;token&gt;&#39;.
     *
     * @throws ApiException if the Api call fails
     */
    @Test
    public void installOnBranchTest() throws ApiException, IOException, URISyntaxException {
        List<String> branch = Arrays.asList("root", "elt1");
        String fileName = "test";
        ProcessGroupFlowEntity response = TestUtils.createProcessGroupFlowEntity("idProcessGroupFlow", "nameProcessGroupFlow");
        when(processGroupServiceMock.createDirectory(branch)).thenReturn(response);
        TemplateEntity template = new TemplateEntity();
        template.setId("idTemplate");
        template.setTemplate(new TemplateDTO());
        template.getTemplate().setGroupId("idProcessGroupFlow");
        template.getTemplate().setId("idTemplate");
        when(processGroupsApiMock.uploadTemplate(anyString(), any())).thenReturn(template);
        //when(processGroupsApiMock.uploadTemplate(processGroupFlow.getId(), new File(fileName))).thenReturn(template);
        TemplatesEntity templatesEntity = new TemplatesEntity();
        when(flowApiMock.getTemplates()).thenReturn(templatesEntity);

        templateService.installOnBranch(branch, fileName, true);

        InstantiateTemplateRequestEntity instantiateTemplate = new InstantiateTemplateRequestEntity(); // InstantiateTemplateRequestEntity | The instantiate template request.
        instantiateTemplate.setTemplateId(template.getId());
        instantiateTemplate.setOriginX(0d);
        instantiateTemplate.setOriginY(0d);

        verify(processGroupServiceMock).createDirectory(branch);
        verify(processGroupsApiMock).uploadTemplate(response.getProcessGroupFlow().getId(), new File(fileName));
        verify(processGroupsApiMock).instantiateTemplate(response.getProcessGroupFlow().getId(), instantiateTemplate);
        verify(templatesApiMock, never()).removeTemplate(template.getTemplate().getId());
    }


    @Test
    public void installOnBranchWithRemoveTemplateTest() throws ApiException, IOException, URISyntaxException {
        List<String> branch = Arrays.asList("root", "elt1");
        String fileName = "test";
        ProcessGroupFlowEntity response = TestUtils.createProcessGroupFlowEntity("idProcessGroupFlow", "nameProcessGroupFlow");
        when(processGroupServiceMock.createDirectory(branch)).thenReturn(response);
        TemplateEntity template = new TemplateEntity();
        template.setId("idTemplate");
        template.setTemplate(new TemplateDTO());
        template.getTemplate().setGroupId("idProcessGroupFlow");
        template.getTemplate().setId("idTemplate");
        when(processGroupsApiMock.uploadTemplate(anyString(), any())).thenReturn(template);
        TemplatesEntity templatesEntity = new TemplatesEntity();
        when(flowApiMock.getTemplates()).thenReturn(templatesEntity);
        //when(processGroupsApiMock.uploadTemplate(processGroupFlow.getId(), new File(fileName))).thenReturn(template);

        templateService.installOnBranch(branch, fileName, false);

        InstantiateTemplateRequestEntity instantiateTemplate = new InstantiateTemplateRequestEntity(); // InstantiateTemplateRequestEntity | The instantiate template request.
        instantiateTemplate.setTemplateId(template.getId());
        instantiateTemplate.setOriginX(0d);
        instantiateTemplate.setOriginY(0d);

        verify(processGroupServiceMock).createDirectory(branch);
        verify(processGroupsApiMock).uploadTemplate(response.getProcessGroupFlow().getId(), new File(fileName));
        verify(processGroupsApiMock).instantiateTemplate(response.getProcessGroupFlow().getId(), instantiateTemplate);
        verify(templatesApiMock).removeTemplate(template.getTemplate().getId());
    }


    @Test
    public void undeployTest() throws ApiException {
        List<String> branch = Arrays.asList("root", "elt1");
        ProcessGroupFlowEntity response = TestUtils.createProcessGroupFlowEntity("idProcessGroupFlow", "nameProcessGroupFlow");
        Optional<ProcessGroupFlowEntity> processGroupFlow = Optional.of(response);

        when(processGroupServiceMock.changeDirectory(branch)).thenReturn(processGroupFlow);

        TemplatesEntity templates = new TemplatesEntity();
        TemplateEntity template = new TemplateEntity();
        template.setId("templateId");
        template.setTemplate(new TemplateDTO());
        template.getTemplate().setGroupId(processGroupFlow.get().getProcessGroupFlow().getId());
        template.getTemplate().setId("templateId");
        templates.addTemplatesItem(template);
        when(flowApiMock.getTemplates()).thenReturn(templates);

        ProcessGroupEntity processGroupEntity = TestUtils.createProcessGroupEntity("idProcessGroupFlow", "nameProcessGroupFlow");
        when(processGroupsApiMock.getProcessGroup(processGroupFlow.get().getProcessGroupFlow().getId())).thenReturn(processGroupEntity);

        when(processGroupsApiMock.removeProcessGroup("idProcessGroupFlow","10", null)).thenReturn(new ProcessGroupEntity());

        Injector injector = Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(TemplatesApi.class).toInstance(templatesApiMock);
                bind(ProcessGroupsApi.class).toInstance(processGroupsApiMock);
                bind(ProcessGroupService.class).toInstance(processGroupServiceMock);
                bind(FlowApi.class).toInstance(flowApiMock);
                bind(Integer.class).annotatedWith(Names.named("timeout")).toInstance(1);
                bind(Integer.class).annotatedWith(Names.named("interval")).toInstance(1);
                bind(Boolean.class).annotatedWith(Names.named("forceMode")).toInstance(false);
                bind(Double.class).annotatedWith(Names.named("placeWidth")).toInstance(1200d);
                bind(PositionDTO.class).annotatedWith(Names.named("startPosition")).toInstance(new PositionDTO());
            }
        });
        injector.getInstance(TemplateService.class).undeploy(branch);
        verify(templatesApiMock).removeTemplate(template.getId());
        verify(processGroupServiceMock).stop(processGroupFlow.get());
        verify(processGroupsApiMock).removeProcessGroup(processGroupFlow.get().getProcessGroupFlow().getId(), "10", null);
    }

    @Test
    public void undeployNoExistTest() throws ApiException {
        List<String> branch = Arrays.asList("root", "elt1");
        Optional<ProcessGroupFlowEntity> processGroupFlow = Optional.empty();
        when(processGroupServiceMock.changeDirectory(branch)).thenReturn(processGroupFlow);
        templateService.undeploy(branch);
        verify(flowApiMock, never()).getTemplates();
    }
}