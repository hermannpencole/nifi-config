package com.github.hermannpencole.nifi.utils;

import com.github.hermannpencole.nifi.config.model.ConfigException;
import com.github.hermannpencole.nifi.config.model.TimeoutException;
import com.github.hermannpencole.nifi.config.utils.FunctionUtils;
import com.github.hermannpencole.nifi.swagger.ApiException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

/**
 * API tests for AccessApi
 */
@RunWith(MockitoJUnitRunner.class)
public class FunctionUtilsTest {

    private int result = 0;

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
        FunctionUtils.runWhile(() -> { throw new IndexOutOfBoundsException();}, 1, -1);
        assertEquals(2, this.result);
    }

    @Test(expected = ConfigException.class)
    public void runWhileConfigExceptionTest() throws ApiException, IOException, URISyntaxException {
        this.result = 0;
        FunctionUtils.runWhile(() -> { throw new ConfigException("test");}, 1, -1);
        assertEquals(2, this.result);
    }
}