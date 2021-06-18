package org.mule.extension.jsonlogger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.mule.extension.jsonlogger.config.JsonLoggerConfig;
import org.mule.extension.jsonlogger.config.LoggerProcessor;
import org.mule.extension.jsonlogger.config.LoggerScopeProcessor;
import org.mule.extension.jsonlogger.mapper.Mapper;
import org.mule.runtime.api.component.location.ComponentLocation;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.time.Duration.between;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.metadata.DataType.TEXT_STRING;

public class JsonLoggerOperations {
  
  protected static Logger log = LoggerFactory.getLogger("org.mule.extension.jsonlogger.JsonLogger");
  private final Result<Void, Void> VOID_RESULT = Result.<Void, Void>builder().build();
  private final Mapper om = new Mapper();
  
  @Inject
  private TransformationService transformationService;
  
  public void logger(@ParameterGroup(name = "Logger")
                     @Expression(value = NOT_SUPPORTED)
                       LoggerProcessor loggerProcessor,
                     @Config
                       JsonLoggerConfig config,
                     ComponentLocation location,
                     CompletionCallback<Void, Void> callback) {
    
    String priority = loggerProcessor.getPriority().toString();
    if (!isLogEnabled(priority)) {
      callback.success(VOID_RESULT);
      return;
    }
    
    ObjectNode logEvent = om.getObjectMapper().createObjectNode();
    
    logEvent.put(Constants.CORRELATION_ID, loggerProcessor.getCorrelationId())
      .put(Constants.MESSAGE, loggerProcessor.getMessage())
      .put(Constants.APPLICATION_NAME, config.getApplicationName())
      .put(Constants.APPLICATION_VERSION, config.getApplicationVersion())
      .put(Constants.ENVIRONMENT, config.getEnvironment())
      .put(Constants.PRIORITY, priority)
      .put(Constants.TRACE_POINT, loggerProcessor.getTracePoint().toString());
    
    addLocationInfo(logEvent, location, config.isLogLocationInfo());
    
    addContent(logEvent, loggerProcessor.getContent(), config);
    
    log(logEvent, priority, config.isPrettyPrint());
    
    callback.success(VOID_RESULT);
  }
  
  private void addContent(ObjectNode logEvent, ParameterResolver<TypedValue<InputStream>> v, JsonLoggerConfig config) {
    TypedValue<InputStream> typedVal = null;
    try {
      typedVal = v.resolve();
      if (typedVal.getValue() == null) {
        return;
      }
      
      if (!config.isParseContentFieldsInJsonOutput() ||
        !typedVal.getDataType().getMediaType().matches(MediaType.APPLICATION_JSON)) {
        logEvent.put(Constants.CONTENT, (String)
          transformationService.transform(typedVal.getValue(),
            typedVal.getDataType(),
            TEXT_STRING));
        
        return;
      }
      
      // Apply data masking only if needed
      ObjectMapper mapper;
      List<String> dataMaskingFields = null;
      if (config.getContentFieldsDataMasking() != null) {
        dataMaskingFields = new ArrayList<>();
        String[] split = config
          .getContentFieldsDataMasking()
          .trim()
          .split(",");
        
        for (String s : split) {
          dataMaskingFields.add(s.trim());
        }
        
      }
      
      if (dataMaskingFields == null || dataMaskingFields.isEmpty()) {
        mapper = om.getObjectMapper();
      } else {
        mapper = om.getObjectMapper(dataMaskingFields);
      }
      
      logEvent.put(Constants.CONTENT, mapper.readTree(typedVal.getValue()));
      
    } catch (Exception e) {
      logEvent.put(Constants.CONTENT, e.getMessage());
    } finally {
      if (typedVal != null &&  typedVal.getValue() != null) {
        try {
          typedVal.getValue().close();
        } catch (Exception e) {
          log.debug("Can't close stream", e);
        }
      }
    }
  }
  
  private void addLocationInfo(ObjectNode logEvent, ComponentLocation location, boolean shouldLogLocation) {
    if (!shouldLogLocation) {
      return;
    }
    Map<String, String> locationInfo = new HashMap<>();
    locationInfo.put(Constants.ROOT_CONTAINER, location.getRootContainerName());
    locationInfo.put(Constants.FILE_NAME, location.getFileName().orElse(""));
    locationInfo.put(Constants.LINE_NUMBER, String.valueOf(location.getLineInFile().orElse(null)));
    logEvent.putPOJO(Constants.LOCATION_INFO, locationInfo);
  }
  
  private void log(ObjectNode logEvent, String priority, boolean isPrettyPrint) {
    ObjectWriter ow = isPrettyPrint ?
      om.getObjectMapper().writer().withDefaultPrettyPrinter() :
      om.getObjectMapper().writer();
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
  
  // scope operations cannot receive config at this time
  public void loggerScope(@ParameterGroup(name = "ScopeLogger")
                          @Expression(value = NOT_SUPPORTED)
                            LoggerScopeProcessor loggerProcessor,
                          ComponentLocation location,
                          Chain operations,
                          CompletionCallback<Object, Object> callback) {
    
    String priority = loggerProcessor.getPriority().toString();
    if (!isLogEnabled(priority)) {
      operations.process(
        callback::success,
        (error, previous) -> {
          callback.error(error);
        });
      return;
    }
    
    ObjectNode logEvent = om.getObjectMapper(new ArrayList<>()).createObjectNode();
    
    Instant startTime = Instant.now();
    
    logEvent.put(Constants.CORRELATION_ID, loggerProcessor.getCorrelationId())
      .put(Constants.TRACE_POINT, loggerProcessor.getScopeTracePoint().toString() + "_BEFORE")
      .put(Constants.PRIORITY, priority)
      .put(Constants.SCOPE_ELAPSED, 0)
      .put(Constants.TIMSTAMP, startTime.toString());
    
    addLocationInfo(logEvent, location, true);
    
    log(logEvent, priority, true);
    
    operations.process(
      result -> {
        Instant endTime = Instant.now();
        logEvent.put(Constants.TRACE_POINT, loggerProcessor.getScopeTracePoint().toString() + "_AFTER")
          .put(Constants.PRIORITY, priority)
          .put(Constants.SCOPE_ELAPSED, between(startTime, endTime).toMillis())
          .put(Constants.TIMSTAMP, endTime.toString());
        
        log(logEvent, priority, true);
        callback.success(result);
      },
      (error, previous) -> {
        Instant endTime = Instant.now();
        logEvent.put(Constants.MESSAGE, error.getMessage())
          .put(Constants.TRACE_POINT, "EXCEPTION_SCOPE")
          .put(Constants.PRIORITY, "ERROR")
          .put(Constants.SCOPE_ELAPSED, between(startTime, endTime).toMillis());
        log(logEvent, "ERROR", true);
        callback.error(error);
      });
  }
}

