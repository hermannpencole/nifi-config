package com.github.hermannpencole.nifi.config.model;

/**
 * Exception for this module
 *
 * Created by SFRJ2737 on 2017-05-28.
 */
public class TimeoutException extends RuntimeException {
    public TimeoutException(Throwable e) {
        super(e);
    }

    public TimeoutException(String s, Throwable e) {
        super(s,e);
    }

    public TimeoutException(String s) {
        super(s);
    }
}
