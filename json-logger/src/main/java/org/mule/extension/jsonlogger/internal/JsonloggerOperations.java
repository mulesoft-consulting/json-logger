package org.mule.extension.jsonlogger.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.joda.time.DateTime;
import org.mule.extension.jsonlogger.api.pojos.LoggerProcessor;
import org.mule.extension.jsonlogger.api.pojos.LoggerProcessorAfter;
import org.mule.extension.jsonlogger.api.pojos.LoggerProcessorBefore;
import org.mule.extension.jsonlogger.api.pojos.LoggerProcessorStart;
import org.mule.extension.jsonlogger.api.pojos.LoggerProcessorError;
import org.mule.extension.jsonlogger.api.pojos.LoggerProcessorEnd;
import org.mule.extension.jsonlogger.api.pojos.LoggerProcessorFlow;
import org.mule.extension.jsonlogger.api.pojos.Priority;
import org.mule.extension.jsonlogger.api.pojos.AfterPriority;
import org.mule.extension.jsonlogger.api.pojos.BeforePriority;
import org.mule.extension.jsonlogger.api.pojos.FlowPriority;
import org.mule.extension.jsonlogger.api.pojos.TracePoint;
import org.mule.extension.jsonlogger.api.pojos.AfterTracePoint;
import org.mule.extension.jsonlogger.api.pojos.BeforeTracePoint;
import org.mule.extension.jsonlogger.api.pojos.ScopeTracePoint;
import org.mule.extension.jsonlogger.internal.datamask.JsonMasker;
import org.mule.extension.jsonlogger.internal.singleton.ConfigsSingleton;
import org.mule.extension.jsonlogger.internal.singleton.LogEventSingleton;
import org.mule.extension.jsonlogger.internal.singleton.ObjectMapperSingleton;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.transformation.TransformationService;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.execution.Execution;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.runtime.operation.FlowListener;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.parameter.CorrelationInfo;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.*;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.metadata.DataType.TEXT_STRING;

/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class JsonloggerOperations {

    /**
     * jsonLogger: JSON Logger output log
     * log: Connector internal log
     */
    protected transient Logger jsonLogger;
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonloggerOperations.class);

    // Void Result for NIO
    private final Result<Void, Void> VOID_RESULT = Result.<Void, Void>builder().build();

    // JSON Object Mapper
    @Inject
    ObjectMapperSingleton om;

    // Log Event for External Destination
    @Inject
    LogEventSingleton logEvent;

    // Global definition of logger configs so that it's available for scope processor (SDK scope doesn't support passing configurations)
    @Inject
    ConfigsSingleton configs;

    // Transformation Service
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

        Long initialTimestamp, loggerTimestamp;
        initialTimestamp = loggerTimestamp = System.currentTimeMillis();

        initLoggerCategory(loggerProcessor.getCategory());

        LOGGER.debug("correlationInfo.getEventId(): " + correlationInfo.getEventId());
        LOGGER.debug("correlationInfo.getCorrelationId(): " + correlationInfo.getCorrelationId());

        try {
            // Add cache entry for initial timestamp based on unique EventId
            initialTimestamp = config.getCachedTimerTimestamp(correlationInfo.getCorrelationId(), initialTimestamp);
        } catch (Exception e) {
            LOGGER.error("initialTimestamp could not be retrieved from the cache config. Defaulting to current System.currentTimeMillis()", e);
        }

        // Calculate elapsed time based on cached initialTimestamp
        Long elapsed = loggerTimestamp - initialTimestamp;

        //config.printTimersKeys();
        if (elapsed == 0) {
            LOGGER.debug("configuring flowListener....");
            flowListener.onComplete(new TimerRemoverRunnable(correlationInfo.getCorrelationId(), config));
        } else {
            LOGGER.debug("flowListener already configured");
        }

        /**
         * Avoid Logger logic execution based on log priority
         */
        if (isLogEnabled(loggerProcessor.getPriority().toString())) {
            // Load disabledFields
            List<String> disabledFields = (config.getJsonOutput().getDisabledFields() != null) ? Arrays.asList(config.getJsonOutput().getDisabledFields().split(",")) : new ArrayList<>();
            LOGGER.debug("The following fields will be disabled for logging: " + disabledFields);

            // Logic to disable fields and/or parse TypedValues as String for JSON log printing
            //Map<String, String> typedValuesAsString = new HashMap<>();
            Map<String, String> typedValuesAsString = new HashMap<>();
            Map<String, JsonNode> typedValuesAsJsonNode = new HashMap<>();
            try {
                PropertyUtils.describe(loggerProcessor).forEach((k, v) -> {
                    if (disabledFields.stream().anyMatch(k::equals)) {
                        try {
                            BeanUtils.setProperty(loggerProcessor, k, null);
                        } catch (Exception e) {
                            LOGGER.error("Failed disabling field: " + k, e);
                        }
                    } else {
                        if (v != null) {
                            try {
                                if (v instanceof ParameterResolver) {
                                    v = ((ParameterResolver) v).resolve();
                                }
                                if (v.getClass().getCanonicalName().equals("org.mule.runtime.api.metadata.TypedValue")) {
                                    LOGGER.debug("org.mule.runtime.api.metadata.TypedValue type was found for field: " + k);
                                    TypedValue<InputStream> typedVal = (TypedValue<InputStream>) v;
                                    LOGGER.debug("Parsing TypedValue for field " + k);

                                    LOGGER.debug("TypedValue MediaType: " + typedVal.getDataType().getMediaType());
                                    LOGGER.debug("TypedValue Type: " + typedVal.getDataType().getType().getCanonicalName());
                                    LOGGER.debug("TypedValue Class: " + typedVal.getValue().getClass().getCanonicalName());

                                    // Remove unparsed field
                                    BeanUtils.setProperty(loggerProcessor, k, null);

                                    // Evaluate if typedValue is null
                                    if (typedVal.getValue() != null) {
                                        // Should content type field be parsed as part of JSON log?
                                        if (config.getJsonOutput().isParseContentFieldsInJsonOutput()) {
                                            // Is content type application/json?
                                            if (typedVal.getDataType().getMediaType().getPrimaryType().equals("application") && typedVal.getDataType().getMediaType().getSubType().equals("json")) {
                                                // Apply masking if needed
                                                List<String> dataMaskingFields = (config.getJsonOutput().getContentFieldsDataMasking() != null) ? Arrays.asList(config.getJsonOutput().getContentFieldsDataMasking().split(",")) : new ArrayList<>();
                                                LOGGER.debug("The following JSON keys/paths will be masked for logging: " + dataMaskingFields);
                                                if (!dataMaskingFields.isEmpty()) {
                                                    JsonNode tempContentNode = om.getObjectMapper().readTree((InputStream)typedVal.getValue());
                                                    JsonMasker masker = new JsonMasker(dataMaskingFields, true);
                                                    JsonNode masked = masker.mask(tempContentNode);
                                                    typedValuesAsJsonNode.put(k, masked);
                                                } else {
                                                    typedValuesAsJsonNode.put(k, om.getObjectMapper().readTree((InputStream)typedVal.getValue()));
                                                }
                                            } else {
                                                typedValuesAsString.put(k, (String) transformationService.transform(typedVal.getValue(), typedVal.getDataType(), TEXT_STRING));
                                            }
                                        } else {
                                            typedValuesAsString.put(k, (String) transformationService.transform(typedVal.getValue(), typedVal.getDataType(), TEXT_STRING));
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                LOGGER.error("Failed parsing field: " + k, e);
                                typedValuesAsString.put(k, "Error parsing expression. See logs for details.");
                            }
                        }
                    }
                });
            } catch (Exception e) {
                LOGGER.error("Unknown error while processing the logger object", e);
            }

            // Aggregate Logger data into mergedLogger
            ObjectNode mergedLogger = om.getObjectMapper().createObjectNode();
            mergedLogger.setAll((ObjectNode) om.getObjectMapper().valueToTree(loggerProcessor));

            /**
             * Custom field ordering for Logger Operation
             * ==========================================
             * This will take place after LoggerProcessor ordering which is defined by the field sequence in loggerProcessor.json
             **/
            // 1. Elapsed Time
            mergedLogger.put("elapsed", elapsed);
            // 2. Location Info: Logger location within Mule application
            if (config.getJsonOutput().isLogLocationInfo()) {
                Map<String, String> locationInfo = locationInfoToMap(location);
                mergedLogger.putPOJO("locationInfo", locationInfo);
            }
            // 3. Timestamp: Add formatted timestamp entry to the logger
            mergedLogger.put("timestamp", getFormattedTimestamp(loggerTimestamp));
            // 4. Content fields: String based fields
            if (!typedValuesAsString.isEmpty()) {
                JsonNode typedValuesNode = om.getObjectMapper().valueToTree(typedValuesAsString);
                mergedLogger.setAll((ObjectNode) typedValuesNode);
            }
            // 5. Content fields: JSONNode based fields
            if (!typedValuesAsJsonNode.isEmpty()) {
                mergedLogger.setAll(typedValuesAsJsonNode);
            }
            // 6. Global info from config
            mergedLogger.setAll((ObjectNode) om.getObjectMapper().valueToTree(config.getGlobalSettings()));
            // 7. Thread Name
            mergedLogger.put("threadName", Thread.currentThread().getName());
            /** End field ordering **/

            /** Print Logger **/
            String finalLog = printObjectToLog(mergedLogger, loggerProcessor.getPriority().toString(), config.getJsonOutput().isPrettyPrint());

            /** Forward Log to External Destination **/
            if (config.getExternalDestination() != null) {
                LOGGER.debug("config.getExternalDestination().getSupportedCategories().isEmpty(): " + config.getExternalDestination().getSupportedCategories().isEmpty());
                LOGGER.debug("config.getExternalDestination().getSupportedCategories().contains(jsonLogger.getName()): " + config.getExternalDestination().getSupportedCategories().contains(jsonLogger.getName()));
                if (configs.getConfig(config.getConfigName()).getExternalDestination().getSupportedCategories().isEmpty() || config.getExternalDestination().getSupportedCategories().contains(jsonLogger.getName())) {
                    LOGGER.debug(jsonLogger.getName() + " is a supported category for external destination");
                    logEvent.publishToExternalDestination(correlationInfo.getEventId(), finalLog, config.getConfigName());
                }
            }
        } else {
            LOGGER.debug("Avoiding logger operation logic execution due to log priority not being enabled");
        }
        callback.success(VOID_RESULT);
    }

    /**
     * Log the START tracepoint at INFO level
     */
    @Execution(ExecutionType.BLOCKING)
    public void logStart(@ParameterGroup(name = "Logger") @Expression(value = NOT_SUPPORTED) LoggerProcessorStart loggerProcessorStart,
                       CorrelationInfo correlationInfo,
                       ComponentLocation location,
                       @Config JsonloggerConfiguration config,
                       FlowListener flowListener,
                       CompletionCallback<Void, Void> callback) {
            
        LoggerProcessor loggerProcessor = new LoggerProcessor();
        loggerProcessor.setCorrelationId(loggerProcessorStart.getCorrelationId());
        loggerProcessor.setMessage(loggerProcessorStart.getMessage());
        loggerProcessor.setContent(loggerProcessorStart.getContent());
        loggerProcessor.setCategory(loggerProcessorStart.getCategory());
        loggerProcessor.setContent(loggerProcessorStart.getContent());
        loggerProcessor.setTracePoint(TracePoint.START);
        loggerProcessor.setPriority(Priority.INFO);
        logger(loggerProcessor, correlationInfo, location, config, flowListener, callback);
    }

    /**
     * Log the END tracepoint at the INFO level
     */
    @Execution(ExecutionType.BLOCKING)
    public void logEnd(@ParameterGroup(name = "Logger") @Expression(value = NOT_SUPPORTED) LoggerProcessorEnd loggerProcessorEnd,
                       CorrelationInfo correlationInfo,
                       ComponentLocation location,
                       @Config JsonloggerConfiguration config,
                       FlowListener flowListener,
                       CompletionCallback<Void, Void> callback) {
            
        LoggerProcessor loggerProcessor = new LoggerProcessor();
        loggerProcessor.setCorrelationId(loggerProcessorEnd.getCorrelationId());
        loggerProcessor.setMessage(loggerProcessorEnd.getMessage());
        loggerProcessor.setContent(loggerProcessorEnd.getContent());
        loggerProcessor.setCategory(loggerProcessorEnd.getCategory());
        loggerProcessor.setContent(loggerProcessorEnd.getContent());
        loggerProcessor.setTracePoint(TracePoint.END);
        loggerProcessor.setPriority(Priority.INFO);
        logger(loggerProcessor, correlationInfo, location, config, flowListener, callback);
    }

    /**
     * Log the BEFORE tracepoint
     */
    @Execution(ExecutionType.BLOCKING)
    public void logBefore(@ParameterGroup(name = "Logger") @Expression(value = NOT_SUPPORTED) LoggerProcessorBefore loggerProcessorBefore,
                       CorrelationInfo correlationInfo,
                       ComponentLocation location,
                       @Config JsonloggerConfiguration config,
                       FlowListener flowListener,
                       CompletionCallback<Void, Void> callback) {
            
        LoggerProcessor loggerProcessor = new LoggerProcessor();
        loggerProcessor.setCorrelationId(loggerProcessorBefore.getCorrelationId());
        loggerProcessor.setMessage(loggerProcessorBefore.getMessage());
        loggerProcessor.setContent(loggerProcessorBefore.getContent());
        loggerProcessor.setCategory(loggerProcessorBefore.getCategory());
        loggerProcessor.setContent(loggerProcessorBefore.getContent());
        loggerProcessor.setTracePoint(TracePoint.valueOf(loggerProcessorBefore.getTracePoint().toString()));
        loggerProcessor.setPriority(Priority.valueOf(loggerProcessorBefore.getPriority().toString()));
        logger(loggerProcessor, correlationInfo, location, config, flowListener, callback);
    }

    /**
     * Log the AFTER tracepoint
     */
    @Execution(ExecutionType.BLOCKING)
    public void logAfter(@ParameterGroup(name = "Logger") @Expression(value = NOT_SUPPORTED) LoggerProcessorAfter loggerProcessorAfter,
                       CorrelationInfo correlationInfo,
                       ComponentLocation location,
                       @Config JsonloggerConfiguration config,
                       FlowListener flowListener,
                       CompletionCallback<Void, Void> callback) {
            
        LoggerProcessor loggerProcessor = new LoggerProcessor();
        loggerProcessor.setCorrelationId(loggerProcessorAfter.getCorrelationId());
        loggerProcessor.setMessage(loggerProcessorAfter.getMessage());
        loggerProcessor.setContent(loggerProcessorAfter.getContent());
        loggerProcessor.setCategory(loggerProcessorAfter.getCategory());
        loggerProcessor.setContent(loggerProcessorAfter.getContent());
        loggerProcessor.setTracePoint(TracePoint.valueOf(loggerProcessorAfter.getTracePoint().toString()));
        loggerProcessor.setPriority(Priority.valueOf(loggerProcessorAfter.getPriority().toString()));
        logger(loggerProcessor, correlationInfo, location, config, flowListener, callback);
    }

    /**
     * Log the EXCEPTION tracepoint at the ERROR level
     */
    @Execution(ExecutionType.BLOCKING)
    public void logError(@ParameterGroup(name = "Logger") @Expression(value = NOT_SUPPORTED) LoggerProcessorError loggerProcessorError,
                       CorrelationInfo correlationInfo,
                       ComponentLocation location,
                       @Config JsonloggerConfiguration config,
                       FlowListener flowListener,
                       CompletionCallback<Void, Void> callback) {
            
        LoggerProcessor loggerProcessor = new LoggerProcessor();
        loggerProcessor.setCorrelationId(loggerProcessorError.getCorrelationId());
        loggerProcessor.setMessage(loggerProcessorError.getMessage());
        loggerProcessor.setContent(loggerProcessorError.getContent());
        loggerProcessor.setCategory(loggerProcessorError.getCategory());
        loggerProcessor.setContent(loggerProcessorError.getContent());
        loggerProcessor.setTracePoint(TracePoint.EXCEPTION);
        loggerProcessor.setPriority(Priority.ERROR);
        logger(loggerProcessor, correlationInfo, location, config, flowListener, callback);
    }

    /**
     * Log the FLOW tracepoint
     */
    @Execution(ExecutionType.BLOCKING)
    public void logFlow(@ParameterGroup(name = "Logger") @Expression(value = NOT_SUPPORTED) LoggerProcessorFlow loggerProcessorFlow,
                       CorrelationInfo correlationInfo,
                       ComponentLocation location,
                       @Config JsonloggerConfiguration config,
                       FlowListener flowListener,
                       CompletionCallback<Void, Void> callback) {
            
        LoggerProcessor loggerProcessor = new LoggerProcessor();
        loggerProcessor.setCorrelationId(loggerProcessorFlow.getCorrelationId());
        loggerProcessor.setMessage(loggerProcessorFlow.getMessage());
        loggerProcessor.setContent(loggerProcessorFlow.getContent());
        loggerProcessor.setCategory(loggerProcessorFlow.getCategory());
        loggerProcessor.setContent(loggerProcessorFlow.getContent());
        loggerProcessor.setTracePoint(TracePoint.FLOW);
        loggerProcessor.setPriority(Priority.valueOf(loggerProcessorFlow.getPriority().toString()));
        logger(loggerProcessor, correlationInfo, location, config, flowListener, callback);
    }

    /**
     * Log scope
     */
    @Execution(ExecutionType.BLOCKING)
    public void loggerScope(@DisplayName("Module configuration") @Example("JSON_Logger_Config") @Summary("Indicate which Global config should be associated with this Scope.") String configurationRef,
                            @Optional(defaultValue="INFO") Priority priority,
                            @Optional(defaultValue="OUTBOUND_REQUEST_SCOPE") ScopeTracePoint scopeTracePoint,
                            @Optional @Summary("If not set, by default will log to the org.mule.extension.jsonlogger.JsonLogger category") String category,
                            @Optional(defaultValue="#[correlationId]") @Placement(tab = "Advanced") String correlationId,
                            ComponentLocation location,
                            CorrelationInfo correlationInfo,
                            FlowListener flowListener,
                            Chain operations,
                            CompletionCallback<Object, Object> callback) {

        /**
         * BEFORE scope logger
         * ===================
         **/

        Long initialTimestamp,loggerTimestamp;
        initialTimestamp = loggerTimestamp = System.currentTimeMillis();

        initLoggerCategory(category);

        LOGGER.debug("correlationInfo.getEventId(): " + correlationInfo.getEventId());
        LOGGER.debug("correlationInfo.getCorrelationId(): " + correlationInfo.getCorrelationId());

        try {
            // Add cache entry for initial timestamp based on unique EventId
            initialTimestamp = configs.getConfig(configurationRef).getCachedTimerTimestamp(correlationInfo.getCorrelationId(), initialTimestamp);
        } catch (Exception e) {
            LOGGER.error("initialTimestamp could not be retrieved from the cache config. Defaulting to current System.currentTimeMillis()", e);
        }

        // Calculate elapsed time based on cached initialTimestamp
        Long elapsed = loggerTimestamp - initialTimestamp;

        //config.printTimersKeys();
        if (elapsed == 0) {
            LOGGER.debug("configuring flowListener....");
            flowListener.onComplete(new TimerRemoverRunnable(correlationInfo.getCorrelationId(), configs.getConfig(configurationRef)));
        } else {
            LOGGER.debug("flowListener already configured");
        }

        /**
         * Avoid Logger Scope logic execution based on log priority
         */
        if (isLogEnabled(priority.toString())) {
            // Execute Scope Logger
            ObjectNode loggerProcessor = om.getObjectMapper().createObjectNode();

            /**
             * Custom field ordering for Logger Scope
             * ===============================
             **/
            loggerProcessor.put("correlationId", correlationId);
            loggerProcessor.put("tracePoint", scopeTracePoint.toString() + "_BEFORE");
            loggerProcessor.put("priority", priority.toString());
            loggerProcessor.put("elapsed", elapsed);
            loggerProcessor.put("scopeElapsed", 0);
            if (configs.getConfig(configurationRef).getJsonOutput().isLogLocationInfo()) {
                Map<String, String> locationInfoMap = locationInfoToMap(location);
                loggerProcessor.putPOJO("locationInfo", locationInfoMap);
            }
            loggerProcessor.put("timestamp", getFormattedTimestamp(loggerTimestamp));
            loggerProcessor.put("applicationName", configs.getConfig(configurationRef).getGlobalSettings().getApplicationName());
            loggerProcessor.put("applicationVersion", configs.getConfig(configurationRef).getGlobalSettings().getApplicationVersion());
            loggerProcessor.put("environment", configs.getConfig(configurationRef).getGlobalSettings().getEnvironment());
            loggerProcessor.put("threadName", Thread.currentThread().getName());

            // Define JSON output formatting
            // Print Logger
            String finalLogBefore = printObjectToLog(loggerProcessor, priority.toString(), configs.getConfig(configurationRef).getJsonOutput().isPrettyPrint());

            // Added temp variable to comply with lambda
            Long finalInitialTimestamp = initialTimestamp;
            operations.process(
                    result -> {

                        /**
                         * AFTER scope logger
                         * ===================
                         **/

                        Long endScopeTimestamp = System.currentTimeMillis();

                        // Calculate elapsed time
                        Long scopeElapsed = endScopeTimestamp - loggerTimestamp;
                        Long mainElapsed = endScopeTimestamp - finalInitialTimestamp;

                        loggerProcessor.put("tracePoint", scopeTracePoint.toString() + "_AFTER");
                        loggerProcessor.put("priority", priority.toString());
                        loggerProcessor.put("elapsed", mainElapsed);
                        loggerProcessor.put("scopeElapsed", scopeElapsed);
                        loggerProcessor.put("timestamp", getFormattedTimestamp(endScopeTimestamp));

                        // Print Logger
                        String finalLogAfter = printObjectToLog(loggerProcessor, priority.toString(), configs.getConfig(configurationRef).getJsonOutput().isPrettyPrint());

                        /** Forward Log to External Destination **/
                        if (configs.getConfig(configurationRef).getExternalDestination() != null) {
                            publishScopeLogEvents(configurationRef, correlationId, finalLogBefore, finalLogAfter);
                        }

                        callback.success(result);
                    },
                    (error, previous) -> {

                        /** ERROR scope logger **/

                        Long errorScopeTimestamp = System.currentTimeMillis();
                        Long mainElapsed = errorScopeTimestamp - finalInitialTimestamp;

                        // Calculate elapsed time
                        Long scopeElapsed = errorScopeTimestamp - loggerTimestamp;

                        loggerProcessor.put("message", "Error found: " + error.getMessage());
                        loggerProcessor.put("tracePoint", "EXCEPTION_SCOPE");
                        loggerProcessor.put("priority", "ERROR");
                        loggerProcessor.put("elapsed", mainElapsed);
                        loggerProcessor.put("scopeElapsed", scopeElapsed);
                        loggerProcessor.put("timestamp", getFormattedTimestamp(errorScopeTimestamp));

                        // Print Logger
                        String finalLogError = printObjectToLog(loggerProcessor, "ERROR", configs.getConfig(configurationRef).getJsonOutput().isPrettyPrint());

                        /** Forward Log to External Destination **/
                        if (configs.getConfig(configurationRef).getExternalDestination() != null) {
                            publishScopeLogEvents(configurationRef, correlationId, finalLogBefore, finalLogError);
                        }

                        callback.error(error);
                    });
        } else {
            // Execute operations without Logger
            LOGGER.debug("Avoiding logger scope logic execution due to log priority not being enabled");
            operations.process(
                    result -> {
                        callback.success(result);
                    },
                    (error, previous) -> {
                        callback.error(error);
                    });
        }
    }

    private void publishScopeLogEvents(String configurationRef, String correlationId, String finalLogBefore, String finalLogAfter) {
        LOGGER.debug("externalDestination.getDestination().getSupportedCategories().isEmpty(): " + configs.getConfig(configurationRef).getExternalDestination().getSupportedCategories().isEmpty());
        LOGGER.debug("externalDestination.getDestination().getSupportedCategories().contains(jsonLogger.getName()): " + configs.getConfig(configurationRef).getExternalDestination().getSupportedCategories().contains(jsonLogger.getName()));
        if (configs.getConfig(configurationRef).getExternalDestination().getSupportedCategories().isEmpty() || configs.getConfig(configurationRef).getExternalDestination().getSupportedCategories().contains(jsonLogger.getName())) {
            LOGGER.debug(jsonLogger.getName() + " is a supported category for external destination");
            // Publishing before and after logEvents for better efficiency
            logEvent.publishToExternalDestination(correlationId, finalLogBefore, configurationRef);
            logEvent.publishToExternalDestination(correlationId, finalLogAfter, configurationRef);
        }
    }

    private Map<String, String> locationInfoToMap(ComponentLocation location) {
        Map<String, String> locationInfo = new HashMap<String, String>();
        //locationInfo.put("location", location.getLocation());
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

    private String printObjectToLog(ObjectNode loggerObj, String priority, boolean isPrettyPrint) {
        ObjectWriter ow = (isPrettyPrint) ? om.getObjectMapper().writer().withDefaultPrettyPrinter() : om.getObjectMapper().writer();
        String logLine = "";
        try {
            logLine = ow.writeValueAsString(loggerObj);
        } catch (Exception e) {
            LOGGER.error("Error parsing log data as a string", e);
        }
        doLog(priority.toString(), logLine);

        return logLine;
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

    private Boolean isLogEnabled(String priority) {
        switch (priority) {
            case "TRACE":
                return jsonLogger.isTraceEnabled();
            case "DEBUG":
                return jsonLogger.isDebugEnabled();
            case "INFO":
                return jsonLogger.isInfoEnabled();
            case "WARN":
                return jsonLogger.isWarnEnabled();
            case "ERROR":
                return jsonLogger.isErrorEnabled();
        }
        return false;
    }

    protected void initLoggerCategory(String category) {
        if (category != null) {
            jsonLogger = LoggerFactory.getLogger(category);
        } else {
            jsonLogger = LoggerFactory.getLogger("org.mule.extension.jsonlogger.JsonLogger");
        }
        LOGGER.debug("category set: " + jsonLogger.getName());
    }

    // Allows executing timer cleanup on flowListener onComplete events
    private static class TimerRemoverRunnable implements Runnable {

        private final String key;
        private final JsonloggerConfiguration config;

        public TimerRemoverRunnable(String key, JsonloggerConfiguration config) {
            this.key = key;
            this.config = config;
        }

        @Override
        public void run() {
            LOGGER.debug("Removing key: " + key);
            config.removeCachedTimerTimestamp(key);
        }
    }
}
