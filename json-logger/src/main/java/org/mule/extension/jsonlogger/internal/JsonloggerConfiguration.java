package org.mule.extension.jsonlogger.internal;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.mule.extension.jsonlogger.api.pojos.LoggerConfig;
import org.mule.runtime.extension.api.annotation.Operations;

import java.util.concurrent.TimeUnit;

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple
 * operations since they represent something core from the extension.
 */
@Operations(JsonloggerOperations.class)
public class JsonloggerConfiguration extends LoggerConfig {

    // Cache methods
    public Long getCachedTimerTimestamp(String key) throws Exception {
        LoadingCache<String, Long> cache = getTimerCache();
        return cache.get(key);
    }

    private static LoadingCache<String, Long> timerCache;

    static {
        timerCache = CacheBuilder.newBuilder()
                .maximumSize(1000000)
                .expireAfterAccess(3, TimeUnit.MINUTES)
                .build(
                        new CacheLoader<String, Long>() {
                            @Override
                            public Long load(String key) throws Exception {
                                return System.currentTimeMillis();
                            }
                        }
                );
    }

    public static LoadingCache<String, Long> getTimerCache() {
        return timerCache;
    }

    public static void invalidateTimerKey (String key) {
        timerCache.invalidate(key);
    }

    public static void printTimerKeys () {
        System.out.println(timerCache.asMap());
    }
}
