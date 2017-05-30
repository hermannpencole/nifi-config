package com.github.hermannpencole.nifi.config;

import com.github.hermannpencole.nifi.config.model.ConfigException;
import com.github.hermannpencole.nifi.config.service.AccessService;
import com.github.hermannpencole.nifi.config.service.ExtractProcessorService;
import com.github.hermannpencole.nifi.config.service.TemplateService;
import com.github.hermannpencole.nifi.config.service.UpdateProcessorService;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.cli.UnrecognizedOptionException;
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

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;


/**
 * Created by SFRJ2737 on 2017-05-28.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({System.class, Guice.class, Main.class})
@PowerMockIgnore("javax.net.ssl.*")
public class MainTest {
    @Mock
    private AccessService accessServiceMock;
    @Mock
    private TemplateService templateServiceMock;
    @Mock
    private UpdateProcessorService updateProcessorServiceMock;
    @Mock
    private ExtractProcessorService extractProcessorServiceMock;

    @Before
    public void init() {
        Main main = new Main();
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
        Mockito.when(Guice.createInjector()).thenReturn(injector);

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
        Mockito.when(Guice.createInjector()).thenReturn(injector);

        Main.main(new String[]{"-nifi","https://localhost:8080/nifi-api","-branch","\"root>N2\"","-m","undeploy","-noVerifySsl"});
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
        Mockito.when(Guice.createInjector()).thenReturn(injector);

        Main.main(new String[]{"-nifi","http://localhost:8080/nifi-api","-branch","\"root>N2\"","-conf","adr","-m","deployTemplate"});
        verify(templateServiceMock).installOnBranch(Arrays.asList("root","N2"), "adr");
    }

    @Test
    public void mainUpdateWithPasswordTest() throws Exception {
        Injector injector = Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(AccessService.class).toInstance(accessServiceMock);
                bind(UpdateProcessorService.class).toInstance(updateProcessorServiceMock);
            }
        });
        //given
        PowerMockito.mockStatic(Guice.class);
        Mockito.when(Guice.createInjector()).thenReturn(injector);

        Main.main(new String[]{"-nifi","http://localhost:8080/nifi-api","-branch","\"root>N2\"","-conf","adr","-m","updateConfig","-u","user","-p","password"});
        verify(updateProcessorServiceMock).updateByBranch(Arrays.asList("root","N2"), "adr");
    }

    @Test
    public void mainExtractWithoutBranchTest() throws Exception {
        Injector injector = Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(AccessService.class).toInstance(accessServiceMock);
                bind(ExtractProcessorService.class).toInstance(extractProcessorServiceMock);
            }
        });
        //given
        PowerMockito.mockStatic(Guice.class);
        Mockito.when(Guice.createInjector()).thenReturn(injector);

        Main.main(new String[]{"-nifi","http://localhost:8080/nifi-api","-conf","adr","-m","extractConfig", "-accessFromTicket"});
        verify(extractProcessorServiceMock).extractByBranch(Arrays.asList("root"), "adr");
    }

    @Test
    public void mainPrintUsageTest() throws Exception {
        PowerMockito.mockStatic(System.class);
        Main.main(new String[]{"-help"});
        PowerMockito.verifyStatic();
        System.exit(1);
    }

    @Test
    public void mainPrintUsageMandatoryTest() throws Exception {
        PowerMockito.mockStatic(System.class);
        Main.main(new String[]{});
        PowerMockito.verifyStatic();
        System.exit(1);
    }

    @Test
    public void mainPrintUsageMandatoryWithoutModeTest() throws Exception {
        PowerMockito.mockStatic(System.class);
        Main.main(new String[]{"-nifi","http://localhost:8080/nifi-api"});
        PowerMockito.verifyStatic();
        System.exit(1);
    }

    @Test
    public void mainPrintUsageMandatoryWithoutConfFileTest() throws Exception {
        PowerMockito.mockStatic(System.class);
        Main.main(new String[]{"-nifi","http://localhost:8080/nifi-api", "-m","extractConfig"});
        PowerMockito.verifyStatic();
        System.exit(1);
    }

    @Test
    public void mainPrintUsageModeUnknowTest() throws Exception {
        PowerMockito.mockStatic(System.class);
        Main.main(new String[]{"-nifi","http://localhost:8080/nifi-api","-branch","\"root>N2\"","-conf","adr","-m","autre"});
        PowerMockito.verifyStatic();
        System.exit(1);
    }
    @Test
    public void mainPrintUsageWithoutPasswordTest() throws Exception {
        PowerMockito.mockStatic(System.class);
        Main.main(new String[]{"-nifi","http://localhost:8080/nifi-api","-branch","\"root>N2\"","-conf","adr","-m","undeploy","-user","user"});
        PowerMockito.verifyStatic();
        System.exit(1);
    }
    @Test
    public void mainPrintUsageWithoutUserTest() throws Exception {
        PowerMockito.mockStatic(System.class);
        Main.main(new String[]{"-nifi","http://localhost:8080/nifi-api","-branch","\"root>N2\"","-conf","adr","-m","undeploy","-password","pass"});
        PowerMockito.verifyStatic();
        System.exit(1);
    }

    @Test(expected = UnrecognizedOptionException.class)
    public void mainPrintUsage5Test() throws Exception {
        Main.main(new String[]{"-nifi","http://localhost:8080/nifi-api", "-branch","\"root>N2\"","-conf","adr","-m","undeploy", "-userErr","user"});
    }

    @Test(expected = ConfigException.class)
    public void mainExceptionTest() throws Exception {
        Injector injector = Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(AccessService.class).toInstance(accessServiceMock);
                bind(TemplateService.class).toInstance(templateServiceMock);
            }
        });
        //given
        PowerMockito.mockStatic(Guice.class);
        Mockito.when(Guice.createInjector()).thenReturn(injector);
        doThrow(new ApiException()).when(accessServiceMock).addTokenOnConfiguration(false, null ,null);
        Main.main(new String[]{"-nifi","http://localhost:8080/nifi-api","-branch","\"root>N2\"","-conf","adr","-m","undeploy"});

    }
}
