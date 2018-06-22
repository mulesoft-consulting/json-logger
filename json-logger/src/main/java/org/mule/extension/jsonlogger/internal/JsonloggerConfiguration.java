package org.mule.extension.jsonlogger.internal;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.mule.extension.jsonlogger.internal.util.GuavaCacheUtil;
import org.mule.extension.jsonlogger.pojos.LoggerConfig;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.operation.FlowListener;
import org.mule.runtime.extension.api.runtime.parameter.CorrelationInfo;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple
 * operations since they represent something core from the extension.
 */
@Operations(JsonloggerOperations.class)
public class JsonloggerConfiguration extends LoggerConfig {

    // Cache methods
    public Long getCachedTimerTimestamp(String key) throws ExecutionException {
        LoadingCache<String, Long> cache = GuavaCacheUtil.getTimerCache();
        //System.out.println(">>> Cache Stats:" +cache.stats());
        //System.out.println(">>> Cache Size:" + cache.size());
        //System.out.println(">>> Cache Map:" + cache.asMap());
        return cache.get(key);
    }

    public void invalidateTimerKey (String key) {
        GuavaCacheUtil.invalidateTimerKey(key);
    }

    public void printTimerKeys () {
        GuavaCacheUtil.printTimerKeys();
    }

}
