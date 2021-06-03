package org.mule.extension.jsonlogger.internal;

import org.mule.extension.jsonlogger.api.pojos.LoggerConfig;
import org.mule.extension.jsonlogger.internal.singleton.ConfigsSingleton;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.RefName;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple
 * operations since they represent something core from the extension.
 */
@Operations(JsonloggerOperations.class)
public class JsonloggerConfiguration extends LoggerConfig implements Initialisable, Disposable {

    @Inject
    ConfigsSingleton configsSingleton;

    @RefName
    private String configName;


    public String getConfigName() {
        return configName;
    }

    /** Timer methods for Elapsed Time **/

    public ConcurrentHashMap<String,Long> timers = new ConcurrentHashMap<String,Long>();

    public ConcurrentHashMap<String, Long> getTimers() { return timers; }

    public Long getCachedTimerTimestamp(String key, Long initialTimeStamp) throws Exception {
        Long startTimestamp = timers.putIfAbsent(key, initialTimeStamp);
        return (startTimestamp == null) ? timers.get(key) : startTimestamp;
    }

    public void removeCachedTimerTimestamp(String key) {
        timers.remove(key);
    }

   
    @Override
    public void initialise() throws InitialisationException {
        configsSingleton.addConfig(configName, this); // Should be refactored once SDK supports passing configs to Scopes
    }

    @Override
    public void dispose() {
    }
}
