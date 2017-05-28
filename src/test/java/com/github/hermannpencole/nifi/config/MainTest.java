package com.github.hermannpencole.nifi.config;

import com.github.hermannpencole.nifi.config.service.AccessService;
import com.github.hermannpencole.nifi.config.service.ExtractProcessorService;
import com.github.hermannpencole.nifi.config.service.TemplateService;
import com.github.hermannpencole.nifi.config.service.UpdateProcessorService;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;

import static org.mockito.Mockito.*;


/**
 * Created by SFRJ2737 on 2017-05-28.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Guice.class)
@PowerMockIgnore("javax.net.ssl.*")
public class MainTest {
    @Mock
    AccessService accessServiceMock;
    @Mock
    TemplateService templateServiceMock;
    @Mock
    UpdateProcessorService updateProcessorServiceMock;
    @Mock
    ExtractProcessorService extractProcessorServiceMock;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void mainUndeployTest() throws Exception {
        Injector injector = Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(AccessService.class).toInstance(accessServiceMock);
                bind(TemplateService.class).toInstance(templateServiceMock);
            }
        });
        //given
        PowerMockito.mockStatic(Guice.class);
        Mockito.when(Guice.createInjector((Module[]) any())).thenReturn(injector);

        Main.main(new String[]{"-nifi","http://localhost:8080/nifi-api","-branch","\"root>N2\"","-conf","adr","-m","undeploy"});
        verify(templateServiceMock).undeploy(Arrays.asList("root","N2"));
    }

    @Test
    public void mainHttpsUndeployTest() throws Exception {
        Injector injector = Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(AccessService.class).toInstance(accessServiceMock);
                bind(TemplateService.class).toInstance(templateServiceMock);
            }
        });
        //given
        PowerMockito.mockStatic(Guice.class);
        Mockito.when(Guice.createInjector((Module[]) any())).thenReturn(injector);

        Main.main(new String[]{"-nifi","https://localhost:8080/nifi-api","-branch","\"root>N2\"","-m","undeploy","noVerifySsl"});
        verify(templateServiceMock).undeploy(Arrays.asList("root","N2"));
    }

    @Test
    public void mainDeployTest() throws Exception {
        Injector injector = Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(AccessService.class).toInstance(accessServiceMock);
                bind(TemplateService.class).toInstance(templateServiceMock);
            }
        });
        //given
        PowerMockito.mockStatic(Guice.class);
        Mockito.when(Guice.createInjector((Module[]) any())).thenReturn(injector);

        Main.main(new String[]{"-nifi","http://localhost:8080/nifi-api","-branch","\"root>N2\"","-conf","adr","-m","deployTemplate"});
        verify(templateServiceMock).installOnBranch(Arrays.asList("root","N2"), "adr");
    }

    @Test
    public void mainUpdateTest() throws Exception {
        Injector injector = Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(AccessService.class).toInstance(accessServiceMock);
                bind(UpdateProcessorService.class).toInstance(updateProcessorServiceMock);
            }
        });
        //given
        PowerMockito.mockStatic(Guice.class);
        Mockito.when(Guice.createInjector((Module[]) any())).thenReturn(injector);

        Main.main(new String[]{"-nifi","http://localhost:8080/nifi-api","-branch","\"root>N2\"","-conf","adr","-m","updateConfig"});
        verify(updateProcessorServiceMock).updateByBranch(Arrays.asList("root","N2"), "adr");
    }

    @Test
    public void mainExtractTest() throws Exception {
        Injector injector = Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(AccessService.class).toInstance(accessServiceMock);
                bind(ExtractProcessorService.class).toInstance(extractProcessorServiceMock);
            }
        });
        //given
        PowerMockito.mockStatic(Guice.class);
        Mockito.when(Guice.createInjector((Module[]) any())).thenReturn(injector);

        Main.main(new String[]{"-nifi","http://localhost:8080/nifi-api","-branch","\"root>N2\"","-conf","adr","-m","extractConfig"});
        verify(extractProcessorServiceMock).extractByBranch(Arrays.asList("root","N2"), "adr");
    }
}
