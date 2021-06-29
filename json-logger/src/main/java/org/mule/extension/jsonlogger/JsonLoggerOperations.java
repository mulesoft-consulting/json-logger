package org.mule.extension.jsonlogger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.mule.extension.jsonlogger.config.JsonLoggerConfig;
import org.mule.extension.jsonlogger.config.LogProcessor;
import org.mule.extension.jsonlogger.config.ScopeProcessor;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.transformation.TransformationService;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static java.time.Duration.between;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.metadata.DataType.TEXT_STRING;

public class JsonLoggerOperations {
  
  private final static Logger log = LoggerFactory.getLogger("org.mule.extension.jsonlogger.JsonLogger");
  private final Result<Void, Void> VOID_RESULT = Result.<Void, Void>builder().build();
  
  @Inject
  private TransformationService transformationService;
  
  public void logger(@ParameterGroup(name = "Logger")
                     @Expression(value = NOT_SUPPORTED)
                       LogProcessor logProcessor,
                     @Config
                       JsonLoggerConfig config,
                     ComponentLocation location,
                     CompletionCallback<Void, Void> callback) {
    
    String priority = logProcessor.getPriority().toString();
    if (!isLogEnabled(priority)) {
      callback.success(VOID_RESULT);
     return;
    }
    ObjectNode logEvent = Mapper.getInstance().createObjectNode();
    
    logEvent.put(Constants.CORRELATION_ID, logProcessor.getCorrelationId())
      .put(Constants.MESSAGE, logProcessor.getMessage())
      .put(Constants.PRIORITY, priority)
      .put(Constants.TRACE_POINT, logProcessor.getTracePoint().toString());
    
    addLocationInfo(logEvent, location);
    
    addAppInfo(logEvent, config);
    
    addContent(logEvent, logProcessor.getContent(), config);
    
    log(logEvent, priority, config.isPrettyPrint());
    
    callback.success(VOID_RESULT);
  }
  
  // scope operations cannot receive config at this time
  public void loggerScope(@ParameterGroup(name = "LoggerScope")
                          @Expression(value = NOT_SUPPORTED)
                            ScopeProcessor scopeProcessor,
                          ComponentLocation location,
                          Chain operations,
                          CompletionCallback<Object, Object> callback) {
    
    String priority = scopeProcessor.getPriority().toString();
    if (!isLogEnabled(priority)) {
      operations.process(
        callback::success,
        (error, previous) -> {
          callback.error(error);
        });
      return;
    }
    
    ObjectNode logEvent = Mapper.getInstance().createObjectNode();
    
    Instant startTime = Instant.now();
    
    logEvent.put(Constants.CORRELATION_ID, scopeProcessor.getCorrelationId())
      .put(Constants.TRACE_POINT, scopeProcessor.getScopeTracePoint().toString() + "_BEFORE")
      .put(Constants.PRIORITY, priority)
      .put(Constants.START_TIME, startTime.toString());
    
    addLocationInfo(logEvent, location);
    
    operations.process(
      result -> {
        Instant endTime = Instant.now();
        logEvent.put(Constants.TRACE_POINT, scopeProcessor.getScopeTracePoint().toString() + "_AFTER")
          .put(Constants.PRIORITY, priority)
          .put(Constants.ELAPSED, between(startTime, endTime).toMillis())
          .put(Constants.END_TIME, endTime.toString());
        
        log(logEvent, priority, true);
        callback.success(result);
      },
      (error, previous) -> {
        Instant endTime = Instant.now();
        logEvent.put(Constants.MESSAGE, error.getMessage())
          .put(Constants.TRACE_POINT, "EXCEPTION_SCOPE")
          .put(Constants.PRIORITY, "ERROR")
          .put(Constants.ELAPSED, between(startTime, endTime).toMillis())
          .put(Constants.END_TIME, endTime.toString());
        log(logEvent, "ERROR", true);
        callback.error(error);
      });
  }
  
  void addContent(ObjectNode logEvent, ParameterResolver<TypedValue<InputStream>> v, JsonLoggerConfig config) {
    TypedValue<InputStream> typedVal = null;
    InputStream inputStream = null;
    try {
      typedVal = v.resolve();
      if (typedVal == null) {
        return;
      }
      inputStream = typedVal.getValue();
      if (inputStream == null ) {
        return;
      }
  
      DataType dataType = typedVal.getDataType();
  
      //if payload is java, then use toString()
      if (MediaType.APPLICATION_JAVA.matches(dataType.getMediaType())) {
        ObjectInputStream ois = new ObjectInputStream(inputStream);
        logEvent.put(Constants.CONTENT, ois.readObject().toString());
        return;
      }
      
      //if payload is anything other java and json, then use transformationService
      if (!MediaType.APPLICATION_JSON.matches(dataType.getMediaType())) {
        logEvent.put(Constants.CONTENT, (String) transformationService.transform(inputStream,
          dataType,
          TEXT_STRING));
        return;
      }
      
      ObjectMapper mapper = Mapper.getInstance(config.getContentFieldsDataMasking());
      log.error(config.getContentFieldsDataMasking());
      logEvent.put(Constants.CONTENT, mapper.readTree(inputStream));
      
    } catch (Exception e) {
      logEvent.put(Constants.CONTENT, e.getMessage());
    } finally {
      if (typedVal != null && inputStream != null) {
        try {
          inputStream.close();
        } catch (Exception e) {
          log.debug("Can't close stream", e);
        }
      }
    }
  }
  
  void addAppInfo(ObjectNode logEvent, JsonLoggerConfig config) {
    logEvent.put(Constants.APPLICATION_NAME, config.getApplicationName());
    logEvent.put(Constants.APPLICATION_VERSION, config.getApplicationVersion());
    logEvent.put(Constants.ENVIRONMENT, config.getEnvironment());
  }
  
  void addLocationInfo(ObjectNode logEvent, ComponentLocation location) {
    Map<String, String> locationInfo = new HashMap<>();
    locationInfo.put(Constants.ROOT_CONTAINER, location.getRootContainerName());
    locationInfo.put(Constants.FILE_NAME, location.getFileName().orElse(""));
    locationInfo.put(Constants.LINE_NUMBER, String.valueOf(location.getLineInFile().orElse(null)));
    logEvent.putPOJO(Constants.LOCATION_INFO, locationInfo);
  }
  
  private void log(ObjectNode logEvent, String priority, boolean isPrettyPrint) {
    ObjectWriter ow = isPrettyPrint ?
      Mapper.getInstance().writer().withDefaultPrettyPrinter() :
      Mapper.getInstance().writer();
    try {
      doLog(priority, ow.writeValueAsString(logEvent));
    } catch (Exception e) {
      String s = String.format("{\"%s\": \"%s\", \"message\": \"Error parsing content as a string: %s\"}",
        Constants.CORRELATION_ID,
        logEvent.get(Constants.CORRELATION_ID),
        e.getMessage());
      log.error(s);
    }
  }
  
  private void doLog(String priority, String logLine) {
    switch (priority) {
      case "TRACE":
        log.trace(logLine);
        break;
      case "DEBUG":
        log.debug(logLine);
        break;
      case "INFO":
        log.info(logLine);
        break;
      case "WARN":
        log.warn(logLine);
        break;
      case "ERROR":
        log.error(logLine);
    }
  }
  
  private Boolean isLogEnabled(String priority) {
    switch (priority) {
      case "TRACE":
        return log.isTraceEnabled();
      case "DEBUG":
        return log.isDebugEnabled();
      case "INFO":
        return log.isInfoEnabled();
      case "WARN":
        return log.isWarnEnabled();
      case "ERROR":
        return log.isErrorEnabled();
    }
    return false;
  }
}

