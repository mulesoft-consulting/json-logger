package org.mule.extension.jsonlogger.internal.destinations.events;

public class LogEvent {

    private String correlationId;
    private String log;
    private String configName;

    public String getLog() {
        return log;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getConfigName() {
        return configName;
    }

    public void setLogEvent(String correlationId, String log, String configName) {
        this.correlationId = correlationId;
        this.log = log;
        this.configName = configName;
    }

}
