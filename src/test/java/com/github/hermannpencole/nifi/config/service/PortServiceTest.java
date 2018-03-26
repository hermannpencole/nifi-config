package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.InputPortsApi;
import com.github.hermannpencole.nifi.swagger.client.OutputPortsApi;
import com.github.hermannpencole.nifi.swagger.client.model.PortDTO;
import com.github.hermannpencole.nifi.swagger.client.model.PortEntity;
import com.github.hermannpencole.nifi.swagger.client.model.PortStatusDTO;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * API tests for AccessApi
 */
@RunWith(MockitoJUnitRunner.class)
public class PortServiceTest {
    @Mock
    private InputPortsApi inputPortsApiMock;

    @Mock
    private OutputPortsApi outputPortsApiMock;

    /**
     * Creates a token for accessing the REST API via username/password
     * <p>
     * The token returned is formatted as a JSON Web Token (JWT). The token is base64 encoded and comprised of three parts. The header, the body, and the signature. The expiration of the token is a contained within the body. The token can be used in the Authorization header in the format &#39;Authorization: Bearer &lt;token&gt;&#39;.
     *
     * @throws ApiException if the Api call fails
     */
    @Test
    public void getByIdInputTest() throws ApiException, IOException, URISyntaxException {
        Injector injector = Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(InputPortsApi.class).toInstance(inputPortsApiMock);
                bind(OutputPortsApi.class).toInstance(outputPortsApiMock);
                bind(Integer.class).annotatedWith(Names.named("timeout")).toInstance(1);
                bind(Integer.class).annotatedWith(Names.named("interval")).toInstance(1);
                bind(Boolean.class).annotatedWith(Names.named("forceMode")).toInstance(false);
            }
        });
        PortService portService = injector.getInstance(PortService.class);
        PortEntity port = new PortEntity();
        port.setComponent(new PortDTO());
        port.getComponent().setId("id");
        when(inputPortsApiMock.getInputPort("id")).thenReturn(port);
        PortEntity portResult = portService.getById("id", PortDTO.TypeEnum.INPUT_PORT);
        assertEquals("id", portResult.getComponent().getId());
    }

    @Test
    public void getByIdOutputTest() throws ApiException, IOException, URISyntaxException {
        Injector injector = Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(InputPortsApi.class).toInstance(inputPortsApiMock);
                bind(OutputPortsApi.class).toInstance(outputPortsApiMock);
                bind(Integer.class).annotatedWith(Names.named("timeout")).toInstance(1);
                bind(Integer.class).annotatedWith(Names.named("interval")).toInstance(1);
                bind(Boolean.class).annotatedWith(Names.named("forceMode")).toInstance(false);
            }
        });
        PortService portService = injector.getInstance(PortService.class);
        PortEntity port = new PortEntity();
        port.setComponent(new PortDTO());
        port.getComponent().setId("id");
        when(outputPortsApiMock.getOutputPort("id")).thenReturn(port);
        PortEntity portResult = portService.getById("id", PortDTO.TypeEnum.OUTPUT_PORT);
        assertEquals("id", portResult.getComponent().getId());
    }

    @Test
    public void setStateTest() throws ApiException, IOException, URISyntaxException {
        Injector injector = Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(InputPortsApi.class).toInstance(inputPortsApiMock);
                bind(OutputPortsApi.class).toInstance(outputPortsApiMock);
                bind(Integer.class).annotatedWith(Names.named("timeout")).toInstance(1);
                bind(Integer.class).annotatedWith(Names.named("interval")).toInstance(1);
                bind(Boolean.class).annotatedWith(Names.named("forceMode")).toInstance(false);
            }
        });
        PortService portService = injector.getInstance(PortService.class);
        PortEntity port = new PortEntity();
        port.setComponent(new PortDTO());
        port.getComponent().setId("id");
        port.getComponent().setState(PortDTO.StateEnum.STOPPED);
        port.setStatus(new PortStatusDTO());
        port.getStatus().setRunStatus("Stopped");
        portService.setState(port, PortDTO.StateEnum.STOPPED);
        verify(inputPortsApiMock,never()).updateInputPort(eq("id"), any());
        verify(outputPortsApiMock,never()).updateOutputPort(eq("id"), any());
    }

    @Test
    public void setStateInputTest() throws ApiException, IOException, URISyntaxException {
        Injector injector = Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(InputPortsApi.class).toInstance(inputPortsApiMock);
                bind(OutputPortsApi.class).toInstance(outputPortsApiMock);
                bind(Integer.class).annotatedWith(Names.named("timeout")).toInstance(1);
                bind(Integer.class).annotatedWith(Names.named("interval")).toInstance(1);
                bind(Boolean.class).annotatedWith(Names.named("forceMode")).toInstance(false);
            }
        });
        PortService portService = injector.getInstance(PortService.class);
        PortEntity portStopped = new PortEntity();
        portStopped.setComponent(new PortDTO());
        portStopped.getComponent().setName("name");
        portStopped.getComponent().setId("id");
        portStopped.getComponent().setState(PortDTO.StateEnum.STOPPED);
        portStopped.setStatus(new PortStatusDTO());
        portStopped.getStatus().setRunStatus("Stopped");
        when(inputPortsApiMock.updateInputPort(eq("id"),any())).thenReturn(portStopped);
        PortEntity port = new PortEntity();
        port.setId("id");
        port.setComponent(new PortDTO());
        port.getComponent().setId("id");
        port.getComponent().setState(PortDTO.StateEnum.RUNNING);
        port.getComponent().setType(PortDTO.TypeEnum.INPUT_PORT);
        port.setStatus(new PortStatusDTO());
        port.getStatus().setRunStatus("Running");
        portService.setState(port, PortDTO.StateEnum.STOPPED);
        verify(inputPortsApiMock, times(1)).updateInputPort(eq("id"), any());
    }

    @Test
    public void setStateOutputTest() throws ApiException, IOException, URISyntaxException {
        Injector injector = Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(InputPortsApi.class).toInstance(inputPortsApiMock);
                bind(OutputPortsApi.class).toInstance(outputPortsApiMock);
                bind(Integer.class).annotatedWith(Names.named("timeout")).toInstance(1);
                bind(Integer.class).annotatedWith(Names.named("interval")).toInstance(1);
                bind(Boolean.class).annotatedWith(Names.named("forceMode")).toInstance(false);
            }
        });
        PortService portService = injector.getInstance(PortService.class);
        PortEntity portStopped = new PortEntity();
        portStopped.setComponent(new PortDTO());
        portStopped.getComponent().setName("name");
        portStopped.getComponent().setId("id");
        portStopped.getComponent().setState(PortDTO.StateEnum.STOPPED);
        portStopped.setStatus(new PortStatusDTO());
        portStopped.getStatus().setRunStatus("Stopped");
        when(outputPortsApiMock.updateOutputPort(eq("id"),any())).thenReturn(portStopped);
        PortEntity port = new PortEntity();
        port.setId("id");
        port.setComponent(new PortDTO());
        port.getComponent().setId("id");
        port.getComponent().setState(PortDTO.StateEnum.RUNNING);
        port.getComponent().setType(PortDTO.TypeEnum.OUTPUT_PORT);
        port.setStatus(new PortStatusDTO());
        port.getStatus().setRunStatus("Running");
        portService.setState(port, PortDTO.StateEnum.STOPPED);
        verify(outputPortsApiMock, times(1)).updateOutputPort(eq("id"), any());
    }
}