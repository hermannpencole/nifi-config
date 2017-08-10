package com.github.hermannpencole.nifi.config.utils;

import com.github.hermannpencole.nifi.config.model.ConfigException;
import com.github.hermannpencole.nifi.config.model.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class FunctionUtils {
    /**
     * The logger.
     */
    private final static Logger LOG = LoggerFactory.getLogger(FunctionUtils.class);

    public static void runTimeout(Runnable function, int timeout) {
        java.util.concurrent.ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future future = executor.submit(function);
        try {
            if (timeout < 0 ) {
                future.get();
            } else {
                future.get(timeout, TimeUnit.SECONDS);
            }
        } catch (ExecutionException e) {
            LOG.debug(e.getMessage(),e);
            future.cancel(true);
            if (e.getCause() instanceof ConfigException) {
                throw (ConfigException)e.getCause();
            }
            throw new ConfigException(e.getCause());
        } catch (InterruptedException e) {
            future.cancel(true);
            throw new ConfigException(e);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            throw new TimeoutException(e);
        } finally {
            executor.shutdownNow();
        }

    }

    public static void runWhile(Supplier<Boolean> function, int interval) {
       while (function.get()) {
           try {
               //interval are in second
               Thread.sleep(interval * 1000);
           } catch (InterruptedException e) {
               throw new ConfigException(e);
           }
       }
    }

    public static void runWhile(Supplier<Boolean> function, int interval, int timeout) {
        runTimeout(() -> runWhile(function, interval ), timeout);
    }
}
