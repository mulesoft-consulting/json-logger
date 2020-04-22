package org.mule.extension.jsonlogger.internal;

import org.mule.extension.jsonlogger.api.pojos.LoggerConfig;
import org.mule.extension.jsonlogger.internal.destinations.Destination;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

import java.util.concurrent.ConcurrentHashMap;

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple
 * operations since they represent something core from the extension.
 */
@Operations(JsonloggerOperations.class)
public class JsonloggerConfiguration extends LoggerConfig {

    @Parameter
    @Optional
    @Placement(tab = "Destinations")
    private Destination externalDestination;

    public Destination getExternalDestination() {
        return externalDestination;
    }

    public void setExternalDestination(Destination externalDestination) {
        this.externalDestination = externalDestination;
    }

    /** Timer methods for Elapsed Time **/

    public ConcurrentHashMap<String,Long> timers = new ConcurrentHashMap<String,Long>();

    public ConcurrentHashMap<String, Long> getTimers() { return timers; }

    public void setTimers(ConcurrentHashMap<String, Long> timers) { this.timers = timers; }

    public void printTimersKeys () {
        System.out.println("Current timers: " + timers);
    }

    public Long getCachedTimerTimestamp(String key, Long initialTimeStamp) throws Exception {
        Long startTimestamp = timers.putIfAbsent(key, initialTimeStamp);
        return (startTimestamp == null) ? timers.get(key) : startTimestamp;
    }

    public void removeCachedTimerTimestamp(String key) {
        timers.remove(key);
    }

}
