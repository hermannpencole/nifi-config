package com.github.hermannpencole.nifi.config.service;

import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.FlowApi;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by SFRJ2737 on 2017-05-28.
 *
 * @author hermann pencolé
 */
@Singleton
public class InformationService {

    @Inject
    private FlowApi flowApi;

    /**
     * get the nifi version.
     *
     * @throws ApiException
     */
    public String getVersion() throws ApiException {
        return flowApi.getAboutInfo().getAbout().getVersion();
    }
}
