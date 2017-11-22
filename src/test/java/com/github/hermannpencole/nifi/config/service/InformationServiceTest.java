package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.FlowApi;
import com.github.hermannpencole.nifi.swagger.client.model.AboutDTO;
import com.github.hermannpencole.nifi.swagger.client.model.AboutEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * API tests for AccessApi
 */
@RunWith(MockitoJUnitRunner.class)
public class InformationServiceTest {
    @Mock
    private FlowApi flowApiMock;

    @InjectMocks
    private InformationService informationService;

    /**
     * Creates a token for accessing the REST API via username/password
     * <p>
     * The token returned is formatted as a JSON Web Token (JWT). The token is base64 encoded and comprised of three parts. The header, the body, and the signature. The expiration of the token is a contained within the body. The token can be used in the Authorization header in the format &#39;Authorization: Bearer &lt;token&gt;&#39;.
     *
     * @throws ApiException if the Api call fails
     */
    @Test
    public void createAccessFromTicketTest() throws ApiException, IOException, URISyntaxException {
        AboutEntity response = new AboutEntity();
        response.setAbout(new AboutDTO());
        response.getAbout().setVersion("version");
        when(flowApiMock.getAboutInfo()).thenReturn(response);
        String result = informationService.getVersion();
        verify(flowApiMock).getAboutInfo();
        assertEquals("version" , result);
    }
}