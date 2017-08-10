package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.config.model.TimeoutException;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.ConnectionsApi;
import com.github.hermannpencole.nifi.swagger.client.ControllerServicesApi;
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

import static org.mockito.Mockito.when;

/**
 * API tests for AccessApi
 */
@RunWith(MockitoJUnitRunner.class)
public class ControllerServiceServiceTests {

    @Mock
    private ControllerServicesApi controllerServicesApi;

    @Test
    //TODO
    public void updateControllerServiceTest() throws ApiException, IOException, URISyntaxException {
        Injector injector = Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(ControllerServicesApi.class).toInstance(controllerServicesApi);
                bind(Integer.class).annotatedWith(Names.named("timeout")).toInstance(1);
                bind(Integer.class).annotatedWith(Names.named("interval")).toInstance(1);
                bind(Boolean.class).annotatedWith(Names.named("forceMode")).toInstance(false);
            }
        });
        ControllerServicesService controllerServicesService = injector.getInstance(ControllerServicesService.class);
        //controllerServicesService.updateControllerService();
    }

}