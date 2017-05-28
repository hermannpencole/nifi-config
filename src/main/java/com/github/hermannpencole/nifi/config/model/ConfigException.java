package com.github.hermannpencole.nifi.config.model;

import com.github.hermannpencole.nifi.swagger.ApiException;

/**
 * Exception for this module
 *
 * Created by SFRJ2737 on 2017-05-28.
 */
public class ConfigException extends RuntimeException {
    public ConfigException(String s, ApiException e) {
        super(s,e);
    }

    public ConfigException(String s) {
        super(s);
    }
}
