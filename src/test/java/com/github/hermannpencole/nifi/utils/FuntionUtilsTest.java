package com.github.hermannpencole.nifi.utils;

import com.github.hermannpencole.nifi.config.model.ConfigException;
import com.github.hermannpencole.nifi.config.model.TimeoutException;
import com.github.hermannpencole.nifi.config.service.ProcessGroupService;
import com.github.hermannpencole.nifi.config.service.TemplateService;
import com.github.hermannpencole.nifi.config.service.TestUtils;
import com.github.hermannpencole.nifi.config.utils.FunctionUtils;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.FlowApi;
import com.github.hermannpencole.nifi.swagger.client.ProcessGroupsApi;
import com.github.hermannpencole.nifi.swagger.client.TemplatesApi;
import com.github.hermannpencole.nifi.swagger.client.model.*;
import jdk.nashorn.internal.objects.annotations.Function;
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * API tests for AccessApi
 */
@RunWith(MockitoJUnitRunner.class)
public class FuntionUtilsTest {


    int result = 0;

    @Test(expected = TimeoutException.class)
    public void runWhileTimeOutTest() throws ApiException, IOException, URISyntaxException {
        FunctionUtils.runWhile(() -> true, 1, 1);
    }

    @Test
    public void runWhileTest() throws ApiException, IOException, URISyntaxException {
        this.result = 0;
        FunctionUtils.runWhile(() -> {this.result +=1; return this.result != 2;}, 1, 10);
        assertEquals(2, this.result);
    }

    @Test(expected = ConfigException.class)
    public void runWhileExceptionTest() throws ApiException, IOException, URISyntaxException {
        this.result = 0;
        FunctionUtils.runWhile(() -> { throw new RuntimeException();}, 1, -1);
        assertEquals(2, this.result);
    }

    @Test(expected = ConfigException.class)
    public void runWhileConfigExceptionTest() throws ApiException, IOException, URISyntaxException {
        this.result = 0;
        FunctionUtils.runWhile(() -> { throw new ConfigException("test");}, 1, -1);
        assertEquals(2, this.result);
    }
}