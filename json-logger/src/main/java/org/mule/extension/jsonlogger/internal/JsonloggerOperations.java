package org.mule.extension.jsonlogger.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.mule.extension.jsonlogger.api.pojos.LoggerProcessor;
import org.mule.extension.jsonlogger.internal.singleton.ObjectMapperSingleton;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.api.metadata.MediaType;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.metadata.DataType.TEXT_STRING;

public class JsonloggerOperations {
  
  protected transient Logger jsonLogger = LoggerFactory.getLogger("org.mule.extension.jsonlogger.JsonLogger");
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
    
    if (!isLogEnabled(loggerProcessor.getPriority().toString())) {
      callback.success(VOID_RESULT);
      return;
    }
    
    Map<String, String> typedValuesAsString = new HashMap<>();
    Map<String, JsonNode> typedValuesAsJsonNode = new HashMap<>();
    
    parseContent(loggerProcessor.getContent(), config);
    //TODO: Create mapper once
    ObjectNode mergedLogger = om.getObjectMapper().createObjectNode();
    mergedLogger.setAll((ObjectNode) om.getObjectMapper().valueToTree(loggerProcessor));
    
    mergedLogger.put("timestamp", getFormattedTimestamp());
    
    if (config.getJsonOutput().isLogLocationInfo()) {
      mergedLogger.putPOJO("locationInfo", locationInfoToMap(location));
    }
    
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
    callback.success(VOID_RESULT);
  }
  
  private void parseContent(ParameterResolver<TypedValue<InputStream>> v, JsonloggerConfiguration config) {
    Map<String, String> typedValuesAsString = new HashMap<>();
    Map<String, JsonNode> typedValuesAsJsonNode = new HashMap<>();
    
    TypedValue<InputStream> typedVal = v.resolve();
    if (typedVal.getValue() == null) {
      return;
    }
    
    if (!config.getJsonOutput().isParseContentFieldsInJsonOutput() ||
      !typedVal.getDataType().getMediaType().matches(MediaType.APPLICATION_JSON)) {
      typedValuesAsString.put(Constants.CONTENT, (String) transformationService.transform(typedVal.getValue(), typedVal.getDataType(), TEXT_STRING));
      return;
    }
    
    // Apply data masking only if needed
    ObjectMapper mapper;
    List<String> dataMaskingFields = null;
    if (config.getJsonOutput().getContentFieldsDataMasking() != null) {
      dataMaskingFields = new ArrayList<>();
      String[] split = config
        .getJsonOutput()
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
    
    try {
      typedValuesAsJsonNode.put(Constants.CONTENT, mapper.readTree(typedVal.getValue()));
    } catch (Exception e) {
      typedValuesAsString.put(Constants.CONTENT, e.getMessage());
    }
  }
  
  private Map<String, String> locationInfoToMap(ComponentLocation location) {
    Map<String, String> locationInfo = new HashMap<>();
    locationInfo.put("rootContainer", location.getRootContainerName());
    locationInfo.put("component", location.getComponentIdentifier().getIdentifier().toString());
    locationInfo.put("fileName", location.getFileName().orElse(""));
    locationInfo.put("lineInFile", String.valueOf(location.getLineInFile().orElse(null)));
    return locationInfo;
  }
  
  private String getFormattedTimestamp() {
    Instant now = Instant.now();
    return now.toString();
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
}
