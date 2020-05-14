package org.mule.extension.jsonlogger.internal;

import org.mule.extension.jsonlogger.api.pojos.LoggerConfig;
import org.mule.extension.jsonlogger.internal.destinations.Destination;
import org.mule.extension.jsonlogger.internal.singleton.ConfigsSingleton;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.RefName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

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

    @Parameter
    @Optional
    @Placement(tab = "Destinations")
    private Destination externalDestination;

    public String getConfigName() {
        return configName;
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

    /** Init configs singleton **/
    public void setExternalDestination(Destination externalDestination) {
        this.externalDestination = externalDestination;
    }

    public Destination getExternalDestination() {
        return externalDestination;
    }

    @Override
    public void initialise() throws InitialisationException {
        if (this.externalDestination != null) {
            this.externalDestination.initialise();
        }
        configsSingleton.addConfig(configName, this); // Should be refactored once SDK supports passing configs to Scopes
    }

    @Override
    public void dispose() {
        if (this.externalDestination != null) {
            this.externalDestination.dispose();
        }
    }
}
