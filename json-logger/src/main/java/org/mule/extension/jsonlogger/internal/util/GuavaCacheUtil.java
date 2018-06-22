package org.mule.extension.jsonlogger.internal.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.TimeUnit;

public class GuavaCacheUtil {

    private static LoadingCache<String, Long> timerCache;
    static {
        timerCache = CacheBuilder.newBuilder()
                .maximumSize(1000000)
                .expireAfterAccess(1, TimeUnit.MINUTES)
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
