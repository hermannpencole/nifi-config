package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.model.TimeoutException;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.ConnectionsApi;
import com.github.hermannpencole.nifi.swagger.client.FlowfileQueuesApi;
import com.github.hermannpencole.nifi.swagger.client.model.*;
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
import static org.mockito.Mockito.when;

/**
 * API tests for AccessApi
 */
@RunWith(MockitoJUnitRunner.class)
public class ConnectableServiceTest {

    @Test
    public void createConnectableDTOFromPortInpputTest() throws ApiException, IOException, URISyntaxException {
        PortEntity port = new PortEntity();
        port.setComponent(new PortDTO());
        port.getComponent().setParentGroupId("parentId");
        port.getComponent().setName("name");
        port.getComponent().setId("id");
        port.getComponent().setType(PortDTO.TypeEnum.INPUT_PORT);
        ConnectableDTO result = (new ConnectableService()).createConnectableDTOFromPort(port);
        assertEquals("parentId", result.getGroupId());
        assertEquals("name", result.getName());
        assertEquals("id", result.getId());
        assertEquals(ConnectableDTO.TypeEnum.INPUT_PORT, result.getType());
    }

    @Test
    public void createConnectableDTOFromPortOutputTest() throws ApiException, IOException, URISyntaxException {
        PortEntity port = new PortEntity();
        port.setComponent(new PortDTO());
        port.getComponent().setParentGroupId("parentId");
        port.getComponent().setName("name");
        port.getComponent().setId("id");
        port.getComponent().setType(PortDTO.TypeEnum.OUTPUT_PORT);
        ConnectableDTO result = (new ConnectableService()).createConnectableDTOFromPort(port);
        assertEquals("parentId", result.getGroupId());
        assertEquals("name", result.getName());
        assertEquals("id", result.getId());
        assertEquals(ConnectableDTO.TypeEnum.OUTPUT_PORT, result.getType());
    }


}