package org.mule.extension.jsonlogger.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.joda.time.DateTime;
import org.mule.extension.jsonlogger.api.pojos.LoggerProcessor;
import org.mule.extension.jsonlogger.api.pojos.Priority;
import org.mule.extension.jsonlogger.api.pojos.ScopeTracePoint;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.transformation.TransformationService;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.execution.Execution;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.runtime.operation.FlowListener;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.parameter.CorrelationInfo;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.metadata.DataType.JSON_STRING;

/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class JsonloggerOperations implements Initialisable {

    /**
     * Create the SLF4J logger
     * jsonLogger: JSON output log
     * log: Connector internal log
     */
    private static final Logger jsonLogger = LoggerFactory.getLogger("org.mule.extension.jsonlogger.JsonLogger");
    private static final Logger log = LoggerFactory.getLogger("org.mule.extension.jsonlogger.JsonLoggerExtension");

    // Void Result for NIO
    private static final Result<Void, Void> VOID_RESULT = Result.<Void, Void>builder().build();

    // JSON Object Mapper
    private static final ObjectMapper om = new ObjectMapper()
                                                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                                                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                                                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    @Inject
    private TransformationService transformationService;

    /**
     * Log a new entry
     */
    @Execution(ExecutionType.BLOCKING)
    public void logger(@ParameterGroup(name = "Logger") @Expression(value = NOT_SUPPORTED) LoggerProcessor loggerProcessor,
                       CorrelationInfo correlationInfo,
                       ComponentLocation location,
                       @Config JsonloggerConfiguration config,
                       FlowListener flowListener,
                       CompletionCallback<Void, Void> callback) {

        Long initialTimestamp,loggerTimestamp;
        initialTimestamp = loggerTimestamp = System.currentTimeMillis();

        try {
            // Add cache entry for initial timestamp based on unique EventId
            initialTimestamp = config.getCachedTimerTimestamp(correlationInfo.getEventId(), initialTimestamp);
        } catch (Exception e) {
            log.error("initialTimestamp could not be retrieved from the cache config. Defaulting to current System.currentTimeMillis()", e);
        }

        // Calculate elapsed time based on cached initialTimestamp
        Long elapsed = loggerTimestamp - initialTimestamp;

        //config.printTimersKeys();
        if (elapsed == 0) {
            log.debug("configuring flowListener....");
            flowListener.onComplete(new TimerRemoverRunnable(correlationInfo.getEventId(), config));
        } else {
            log.debug("flowListener already configured");
        }

        // Load disabledFields
        List<String> disabledFields = (config.getJsonOutput().getDisabledFields() != null) ? Arrays.asList(config.getJsonOutput().getDisabledFields().split(",")) : new ArrayList<>();
        log.debug("The following fields will be disabled for logging: " + disabledFields);

        // Logic to disable fields and/or parse TypedValues as String for JSON log printing
        Map<String, String> typedValuesAsString = new HashMap<>();
        try {
            PropertyUtils.describe(loggerProcessor).forEach((k, v) -> {
                if (disabledFields.stream().anyMatch(k::equals)) {
                    try {
                        BeanUtils.setProperty(loggerProcessor, k, null);
                    } catch (Exception e) {
                        log.error("Failed disabling field: " + k, e);
                    }
                } else {
                    if (v != null) {
                        try {
                            if(v instanceof ParameterResolver) {
                                v = ((ParameterResolver) v).resolve();
                            }
                            if (v.getClass().getCanonicalName().equals("org.mule.runtime.api.metadata.TypedValue")) {
                                log.debug("org.mule.runtime.api.metadata.TypedValue type was found for field: " + k);
                                TypedValue<Object> typedVal = (TypedValue<Object>) v;
                                DataType dataType = typedVal.getDataType();
                                String stringifiedVal;
                                Object originalVal = typedVal.getValue();
                                log.debug("Parsing TypedValue for field " + k + " as string for logging...");
                                if (originalVal.getClass().getSimpleName().equals("String")) {
                                    stringifiedVal = (String) originalVal;
                                } else {
                                    stringifiedVal = (String) transformationService.transform(originalVal, dataType, JSON_STRING);
                                }
                                // Removing un-parsed value
                                BeanUtils.setProperty(loggerProcessor, k, null);

                                typedValuesAsString.put(k, stringifiedVal);
                            }
                        } catch (Exception e) {
                            log.error("Failed parsing field: " + k, e);
                            typedValuesAsString.put(k, "Error parsing expression. See logs for details.");
                        }
                    }
                }
            });
        } catch (Exception e) {
            log.error("Unknown error while processing the logger object", e);
        }

        // Merge contents of loggerProcessor and globalSettings
        JsonNode nodeLoggerJson = om.valueToTree(loggerProcessor);
        JsonNode nodeConfigJson = om.valueToTree(config.getGlobalSettings());
        ObjectNode mergedLogger = (ObjectNode) merge(nodeLoggerJson, nodeConfigJson);

        // Adding typedValue fields
        JsonNode typedValuesNode = om.valueToTree(typedValuesAsString);
        mergedLogger.setAll((ObjectNode) typedValuesNode);

        /* Adding additional metadata
            - elapsed
            - threadName
            - locationInfo
         */
        mergedLogger.put("elapsed", Long.toString(elapsed));

        mergedLogger.put("threadName", Thread.currentThread().getName());

        // Evaluate if location info should be logged
        if (config.getJsonOutput().isLogLocationInfo()) {
            // LocationInfo
            Map<String, String> locationInfo = locationInfoToMap(location);
            mergedLogger.putPOJO("locationInfo", locationInfo);
        }

        // Add formatted timestamp entry to the logger
        mergedLogger.put("timestamp", getFormattedTimestamp(loggerTimestamp));

        // Print Logger
        printObjectToLog(mergedLogger, loggerProcessor.getPriority().toString(), config.getJsonOutput().isPrettyPrint());

        callback.success(VOID_RESULT);
    }

    /**
     * Log a new entry
     */
    @Execution(ExecutionType.BLOCKING)
    public void loggerScope(@Optional(defaultValue="INFO") Priority priority,
                            @Optional(defaultValue="OUTBOUND_REQUEST_SCOPE") ScopeTracePoint scopeTracePoint,
                            @Optional(defaultValue="true") boolean locationInfo,
                            @Optional(defaultValue="true") boolean prettyPrint,
                            @Optional(defaultValue="#[correlationId]") @Placement(tab = "Advanced") String correlationId,
                            CorrelationInfo correlationInfo,
                            ComponentLocation location,
                            Chain operations,
                            CompletionCallback<Object, Object> callback) {

        /** BEFORE scope logger **/

        Long initialTimestamp = System.currentTimeMillis();

        ObjectNode beforeLoggerProcessor = om.createObjectNode();
        ObjectNode afterLoggerProcessor = om.createObjectNode();

        beforeLoggerProcessor.put("correlationId", correlationId);
        beforeLoggerProcessor.put("priority", priority.toString());
        beforeLoggerProcessor.put("message", "Before " + scopeTracePoint);
        beforeLoggerProcessor.put("tracePoint", scopeTracePoint.toString());
        beforeLoggerProcessor.put("scopeElapsed", 0);
        beforeLoggerProcessor.put("threadName", Thread.currentThread().getName());
        beforeLoggerProcessor.put("timestamp", getFormattedTimestamp(initialTimestamp));
        if (locationInfo) {
            Map<String, String> locationInfoMap = locationInfoToMap(location);
            beforeLoggerProcessor.putPOJO("locationInfo", locationInfoMap);
        }

        // Define JSON output formatting
        final boolean isPrettyPrint;
        if (System.getProperty("json.logger.scope.prettyPrint") != null && !System.getProperty("json.logger.scope.prettyPrint").equals("")) {
            isPrettyPrint = Boolean.parseBoolean(System.getProperty("json.logger.scope.prettyPrint"));
        } else {
            isPrettyPrint = prettyPrint;
        }

        // Print Logger
        printObjectToLog(beforeLoggerProcessor, priority.toString(), isPrettyPrint);

        operations.process(
                result -> {

                    /** AFTER scope logger **/

                    Long endTimestamp = System.currentTimeMillis();

                    // Calculate elapsed time
                    Long elapsed = endTimestamp - initialTimestamp;

                    afterLoggerProcessor.put("correlationId", correlationId);
                    afterLoggerProcessor.put("priority", priority.toString());
                    afterLoggerProcessor.put("message", "After " + scopeTracePoint);
                    afterLoggerProcessor.put("tracePoint", scopeTracePoint.toString());
                    afterLoggerProcessor.put("scopeElapsed", elapsed);
                    afterLoggerProcessor.put("threadName", Thread.currentThread().getName());
                    afterLoggerProcessor.put("timestamp", getFormattedTimestamp(endTimestamp));
                    if (locationInfo) {
                        Map<String, String> locationInfoMap = locationInfoToMap(location);
                        afterLoggerProcessor.putPOJO("locationInfo", locationInfoMap);
                    }

                    // Print Logger
                    printObjectToLog(afterLoggerProcessor, priority.toString(), isPrettyPrint);

                    callback.success(result);
                },
                (error, previous) -> {

                    /** ERROR scope logger **/

                    ObjectNode errorLoggerProcessor = om.createObjectNode();

                    Long errorTimestamp = System.currentTimeMillis();

                    // Calculate elapsed time
                    Long elapsed = errorTimestamp - initialTimestamp;

                    errorLoggerProcessor.put("correlationId", correlationId);
                    errorLoggerProcessor.put("priority", "ERROR");
                    errorLoggerProcessor.put("message", "Error found: " + error.getMessage());
                    errorLoggerProcessor.put("tracePoint", "EXCEPTION");
                    errorLoggerProcessor.put("scopeElapsed", elapsed);
                    errorLoggerProcessor.put("threadName", Thread.currentThread().getName());
                    errorLoggerProcessor.put("timestamp", getFormattedTimestamp(errorTimestamp));
                    if (locationInfo) {
                        Map<String, String> locationInfoMap = locationInfoToMap(location);
                        errorLoggerProcessor.putPOJO("locationInfo", locationInfoMap);
                    }

                    // Print Logger
                    printObjectToLog(errorLoggerProcessor, "ERROR", isPrettyPrint);

                    callback.error(error);
                });
    }

    private Map<String, String> locationInfoToMap(ComponentLocation location) {
        Map<String, String> locationInfo = new HashMap<String, String>();
        locationInfo.put("location", location.getLocation());
        locationInfo.put("rootContainer", location.getRootContainerName());
        locationInfo.put("component", location.getComponentIdentifier().getIdentifier().toString());
        locationInfo.put("fileName", location.getFileName().orElse(""));
        locationInfo.put("lineInFile", String.valueOf(location.getLineInFile().orElse(null)));
        return locationInfo;
    }

    private String getFormattedTimestamp(Long loggerTimestamp) {
    /*
        Define timestamp:
        - DateTime: Defaults to ISO format
        - TimeZone: Defaults to UTC. Refer to https://en.wikipedia.org/wiki/List_of_tz_database_time_zones for valid timezones
    */
        DateTime dateTime = new DateTime(loggerTimestamp).withZone(org.joda.time.DateTimeZone.forID(System.getProperty("json.logger.timezone", "UTC")));
        String timestamp = dateTime.toString();
        if (System.getProperty("json.logger.dateformat") != null && !System.getProperty("json.logger.dateformat").equals("")) {
            timestamp = dateTime.toString(System.getProperty("json.logger.dateformat"));
        }
        return timestamp;
    }

    private void printObjectToLog(ObjectNode loggerObj, String priority, boolean isPrettyPrint) {
        ObjectWriter ow = (isPrettyPrint) ? om.writer().withDefaultPrettyPrinter() : om.writer();
        String logLine = "";
        try {
            // JsonNode to Object for key sorting
            final Object obj = om.treeToValue(loggerObj, Object.class);
            logLine = ow.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Error parsing log data as a string", e);
        }
        doLog(priority.toString(), logLine);
    }

    private void doLog(String priority, String logLine) {
        switch (priority) {
            case "TRACE":
                jsonLogger.trace(logLine);
                break;
            case "DEBUG":
                jsonLogger.debug(logLine);
                break;
            case "INFO":
                jsonLogger.info(logLine);
                break;
            case "WARN":
                jsonLogger.warn(logLine);
                break;
            case "ERROR":
                jsonLogger.error(logLine);
                break;
        }
    }

    @Override
    public void initialise() throws InitialisationException {
    }

    private static JsonNode merge(JsonNode mainNode, JsonNode updateNode) {

        Iterator<String> fieldNames = updateNode.fieldNames();

        while (fieldNames.hasNext()) {
            String updatedFieldName = fieldNames.next();
            JsonNode valueToBeUpdated = mainNode.get(updatedFieldName);
            JsonNode updatedValue = updateNode.get(updatedFieldName);

            // If the node is an @ArrayNode
            if (valueToBeUpdated != null && valueToBeUpdated.isArray() &&
                    updatedValue.isArray()) {
                // running a loop for all elements of the updated ArrayNode
                for (int i = 0; i < updatedValue.size(); i++) {
                    JsonNode updatedChildNode = updatedValue.get(i);
                    // Create a new Node in the node that should be updated, if there was no corresponding node in it
                    // Use-case - where the updateNode will have a new element in its Array
                    if (valueToBeUpdated.size() <= i) {
                        ((ArrayNode) valueToBeUpdated).add(updatedChildNode);
                    }
                    // getting reference for the node to be updated
                    JsonNode childNodeToBeUpdated = valueToBeUpdated.get(i);
                    merge(childNodeToBeUpdated, updatedChildNode);
                }
                // if the Node is an @ObjectNode
            } else if (valueToBeUpdated != null && valueToBeUpdated.isObject()) {
                merge(valueToBeUpdated, updatedValue);
            } else {
                if (mainNode instanceof ObjectNode) {
                    ((ObjectNode) mainNode).replace(updatedFieldName, updatedValue);
                }
            }
        }
        return mainNode;
    }

    private static class TimerRemoverRunnable implements Runnable {

        private final String key;
        private final JsonloggerConfiguration config;

        public TimerRemoverRunnable(String key, JsonloggerConfiguration config) {
            this.key = key;
            this.config = config;
        }

        @Override
        public void run() {
            log.debug("Removing key: " + key);
            config.removeCachedTimerTimestamp(key);
        }
    }
}
