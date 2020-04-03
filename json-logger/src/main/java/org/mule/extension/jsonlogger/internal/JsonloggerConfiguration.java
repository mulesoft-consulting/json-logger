package org.mule.extension.jsonlogger.internal;

import org.mule.extension.jsonlogger.api.pojos.LoggerConfig;
import org.mule.extension.jsonlogger.internal.destinations.ExternalDestinations;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.util.concurrent.ConcurrentHashMap;

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple
 * operations since they represent something core from the extension.
 */
@Operations(JsonloggerOperations.class)
public class JsonloggerConfiguration extends LoggerConfig {

    @Parameter
    @Expression(ExpressionSupport.NOT_SUPPORTED)
    @ParameterGroup(name = "External Destination")
    @Summary("Provide a configuration reference for external distribution. Only one destination can be defined.")
    private ExternalDestinations externalDestinations;

    @Parameter
    @Placement(tab = "Destinations")
    @Optional
    @Summary("Name of the target destination (e.g. logger-queue, logger-exchange)")
    private String destination;

    @Parameter
    @Placement(tab = "Destinations")
    @Optional
    @Summary("Indicate which categories should be send externally. If empty, all will be send.")
    private String categoriesForExternalDestination;

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public ExternalDestinations getExternalDestinations() {
        return externalDestinations;
    }

    public void setExternalDestinations(ExternalDestinations externalDestinations) {
        this.externalDestinations = externalDestinations;
    }

    public String getCategoriesForExternalDestination() {
        return categoriesForExternalDestination;
    }

    public void setCategoriesForExternalDestination(String categoriesForExternalDestination) {
        this.categoriesForExternalDestination = categoriesForExternalDestination;
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
