package org.mule.extension.jsonlogger.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.joda.time.DateTime;
import org.mule.extension.jsonlogger.api.pojos.LoggerProcessor;
import org.mule.extension.jsonlogger.internal.singleton.ObjectMapperSingleton;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.transformation.TransformationService;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.execution.Execution;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.parameter.CorrelationInfo;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.metadata.DataType.TEXT_STRING;

public class JsonloggerOperations {
  
  protected transient Logger jsonLogger;
  private static final Logger LOGGER = LoggerFactory.getLogger(JsonloggerOperations.class);
  
  // Void Result for NIO
  private final Result<Void, Void> VOID_RESULT = Result.<Void, Void>builder().build();
  
  @Inject
  ObjectMapperSingleton om;
  
  @Inject
  private TransformationService transformationService;
  
  @Execution(ExecutionType.BLOCKING)
  public void logger(@ParameterGroup(name = "Logger") @Expression(value = NOT_SUPPORTED) LoggerProcessor loggerProcessor,
                     CorrelationInfo correlationInfo,
                     ComponentLocation location,
                     @Config JsonloggerConfiguration config,
                     CompletionCallback<Void, Void> callback) {
    
    initLoggerCategory(loggerProcessor.getCategory());
    
    if (!isLogEnabled(loggerProcessor.getPriority().toString())) {
      callback.success(VOID_RESULT);
      return;
    }
    
    Map<String, String> typedValuesAsString = new HashMap<>();
    Map<String, JsonNode> typedValuesAsJsonNode = new HashMap<>();
    
    Map<String, Object> properties = null;
    try {
      properties = PropertyUtils.describe(loggerProcessor);
    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      LOGGER.error("", e);
    }
    
    if (properties == null) {
      callback.success(VOID_RESULT);
      return;
    }
    
    properties.forEach((k, v) -> {
      if (v == null) {
        return;
      }
      
      try {
        if (v instanceof ParameterResolver) {
          v = ((ParameterResolver) v).resolve();
        }
        
        if (!v.getClass().getCanonicalName().equals("org.mule.runtime.api.metadata.TypedValue")) {
          return;
        }
        
        TypedValue<InputStream> typedVal = (TypedValue<InputStream>) v;
        if (typedVal.getValue() == null) {
          return;
        }
        
        //TODO: what is this doing
        BeanUtils.setProperty(loggerProcessor, k, null);
        
        if (!config.getJsonOutput().isParseContentFieldsInJsonOutput()) {
          typedValuesAsString.put(k, (String) transformationService.transform(typedVal.getValue(), typedVal.getDataType(), TEXT_STRING));
          return;
        }
        
        if (typedVal.getDataType().getMediaType().getPrimaryType().equals("application")
          && typedVal.getDataType().getMediaType().getSubType().equals("json")) {
          
          // Apply masking if needed
          List<String> dataMaskingFields = new ArrayList<>();
          if (config.getJsonOutput().getContentFieldsDataMasking() != null) {
            String[] split = config.getJsonOutput().getContentFieldsDataMasking().split(",");
            for (String s : split) {
              dataMaskingFields.add(s.trim());
            }
          }
          
          if (dataMaskingFields.isEmpty()) {
            typedValuesAsJsonNode.put(k, om.getObjectMapper().readTree(typedVal.getValue()));
          } else {
            JsonNode tempContentNode = om.getObjectMapper(dataMaskingFields).readTree(typedVal.getValue());
            typedValuesAsJsonNode.put(k, tempContentNode);
          }
        } else {
          typedValuesAsString.put(k, (String) transformationService.transform(typedVal.getValue(), typedVal.getDataType(), TEXT_STRING));
        }
        
      } catch (Exception e) {
        LOGGER.error("Failed to parse: " + k, e);
        typedValuesAsString.put(k, "Error parsing expression. See logs for details.");
      }
    });
    
    //TODO: Create mapper once
    ObjectNode mergedLogger = om.getObjectMapper().createObjectNode();
    mergedLogger.setAll((ObjectNode) om.getObjectMapper().valueToTree(loggerProcessor));
    
    mergedLogger.put("elapsed", elapsed);
    
    if (config.getJsonOutput().isLogLocationInfo()) {
      Map<String, String> locationInfo = locationInfoToMap(location);
      mergedLogger.putPOJO("locationInfo", locationInfo);
    }
    
    mergedLogger.put("timestamp", getFormattedTimestamp(loggerTimestamp));
    
    if (!typedValuesAsString.isEmpty()) {
      JsonNode typedValuesNode = om.getObjectMapper(new ArrayList<>()).valueToTree(typedValuesAsString);
      mergedLogger.setAll((ObjectNode) typedValuesNode);
    }
    
    if (!typedValuesAsJsonNode.isEmpty()) {
      mergedLogger.setAll(typedValuesAsJsonNode);
    }
    
    mergedLogger.setAll((ObjectNode) om.getObjectMapper(new ArrayList<>()).valueToTree(config.getGlobalSettings()));
    
    mergedLogger.put("threadName", Thread.currentThread().getName());
    
    printObjectToLog(mergedLogger, loggerProcessor.getPriority().toString(), config.getJsonOutput().isPrettyPrint());
    
  }
  
  private Map<String, String> locationInfoToMap(ComponentLocation location) {
    Map<String, String> locationInfo = new HashMap<>();
    locationInfo.put("rootContainer", location.getRootContainerName());
    locationInfo.put("component", location.getComponentIdentifier().getIdentifier().toString());
    locationInfo.put("fileName", location.getFileName().orElse(""));
    locationInfo.put("lineInFile", String.valueOf(location.getLineInFile().orElse(null)));
    return locationInfo;
  }
  
  private String getFormattedTimestamp(Long loggerTimestamp) {
    DateTime dateTime = new DateTime(loggerTimestamp)
      .withZone(org.joda.time.DateTimeZone.forID(System.getProperty("json.logger.timezone", "UTC")));
    String timestamp;
    if (System.getProperty("json.logger.dateformat") != null && !System.getProperty("json.logger.dateformat").isEmpty()) {
      timestamp = dateTime.toString(System.getProperty("json.logger.dateformat"));
    } else {
      timestamp = dateTime.toString();
    }
    return timestamp;
  }
  
  private void printObjectToLog(ObjectNode loggerObj, String priority, boolean isPrettyPrint) {
    ObjectWriter ow = (isPrettyPrint) ? om.getObjectMapper(new ArrayList<>()).writer().withDefaultPrettyPrinter() : om.getObjectMapper(new ArrayList<>()).writer();
    try {
      String logLine = ow.writeValueAsString(loggerObj);
      doLog(priority, logLine);
    } catch (Exception e) {
      LOGGER.error("Error parsing log data as a string", e);
    }
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
