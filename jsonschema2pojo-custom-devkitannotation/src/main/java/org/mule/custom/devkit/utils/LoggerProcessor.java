
package org.mule.custom.devkit.utils;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.mule.api.annotations.Ignore;
import org.mule.api.annotations.param.Default;


/**
 * Definition for fields used in the logger message processor
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "applicationName",
    "source",
    "startTimestamp",
    "correlationId",
    "priority",
    "message",
    "elapsed",
    "threadName"
})
public class LoggerProcessor {

    @JsonProperty("applicationName")
    @Default("should be defined on the connector")
    @Ignore
    private String applicationName;
    @JsonProperty("source")
    @Default("should be defined on the connector")
    @Ignore
    private String source;
    @JsonProperty("startTimestamp")
    @Default("should be defined on the connector")
    @Ignore
    private String startTimestamp;
    @JsonProperty("correlationId")
    @Default("#[message.id]")
    private String correlationId;
    @JsonProperty("priority")
    @Default("INFO")
    private LoggerProcessor.Priority priority;
    @JsonProperty("message")
    @Default("")
    private String message;
    @JsonProperty("elapsed")
    @Default("")
    private String elapsed;
    @JsonProperty("threadName")
    @Default("#[Thread.currentThread().getName()]")
    private String threadName;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("applicationName")
    public String getApplicationName() {
        return applicationName;
    }

    @JsonProperty("applicationName")
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    @JsonProperty("source")
    public String getSource() {
        return source;
    }

    @JsonProperty("source")
    public void setSource(String source) {
        this.source = source;
    }

    @JsonProperty("startTimestamp")
    public String getStartTimestamp() {
        return startTimestamp;
    }

    @JsonProperty("startTimestamp")
    public void setStartTimestamp(String startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    @JsonProperty("correlationId")
    public String getCorrelationId() {
        return correlationId;
    }

    @JsonProperty("correlationId")
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    @JsonProperty("priority")
    public LoggerProcessor.Priority getPriority() {
        return priority;
    }

    @JsonProperty("priority")
    public void setPriority(LoggerProcessor.Priority priority) {
        this.priority = priority;
    }

    @JsonProperty("message")
    public String getMessage() {
        return message;
    }

    @JsonProperty("message")
    public void setMessage(String message) {
        this.message = message;
    }

    @JsonProperty("elapsed")
    public String getElapsed() {
        return elapsed;
    }

    @JsonProperty("elapsed")
    public void setElapsed(String elapsed) {
        this.elapsed = elapsed;
    }

    @JsonProperty("threadName")
    public String getThreadName() {
        return threadName;
    }

    @JsonProperty("threadName")
    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(applicationName).append(source).append(startTimestamp).append(correlationId).append(priority).append(message).append(elapsed).append(threadName).append(additionalProperties).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof LoggerProcessor) == false) {
            return false;
        }
        LoggerProcessor rhs = ((LoggerProcessor) other);
        return new EqualsBuilder().append(applicationName, rhs.applicationName).append(source, rhs.source).append(startTimestamp, rhs.startTimestamp).append(correlationId, rhs.correlationId).append(priority, rhs.priority).append(message, rhs.message).append(elapsed, rhs.elapsed).append(threadName, rhs.threadName).append(additionalProperties, rhs.additionalProperties).isEquals();
    }

    public enum Priority {

        DEBUG("DEBUG"),
        INFO("INFO"),
        WARN("WARN"),
        ERROR("ERROR");
        private final String value;
        private final static Map<String, LoggerProcessor.Priority> CONSTANTS = new HashMap<String, LoggerProcessor.Priority>();

        static {
            for (LoggerProcessor.Priority c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private Priority(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static LoggerProcessor.Priority fromValue(String value) {
            LoggerProcessor.Priority constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
