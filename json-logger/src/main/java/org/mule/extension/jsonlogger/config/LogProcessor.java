package org.mule.extension.jsonlogger.config;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;

import java.io.InputStream;

public class LogProcessor {
  
  @Parameter
  @Optional(defaultValue = "#[correlationId]")
  @Placement(tab = "Advanced")
  private String correlationId;
  
  @Parameter
  @Optional(defaultValue = "")
  @Summary("Message to be logged")
  @Example("Add a log message")
  private String message;
  
  @Parameter
  @Optional(defaultValue = "{}")
  @Summary("NOTE: Writing the entire payload every time across your application can cause serious performance issues")
  @Content
  private ParameterResolver<TypedValue<InputStream>> content;
  
  @Parameter
  @Optional(defaultValue = "START")
  @Summary("Current processing stage")
  private TracePoint tracePoint;
  
  @Parameter
  @Optional(defaultValue = "INFO")
  @Summary("Logger priority")
  private Priority priority;
  
  public String getCorrelationId() {
    return correlationId;
  }
  
  public LogProcessor setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
    return this;
  }
  
  public String getMessage() {
    return message;
  }
  
  public LogProcessor setMessage(String message) {
    this.message = message;
    return this;
  }
  
  public ParameterResolver<TypedValue<InputStream>> getContent() {
    return content;
  }
  
  public LogProcessor setContent(ParameterResolver<TypedValue<InputStream>> content) {
    this.content = content;
    return this;
  }
  
  public TracePoint getTracePoint() {
    return tracePoint;
  }
  
  public LogProcessor setTracePoint(TracePoint tracePoint) {
    this.tracePoint = tracePoint;
    return this;
  }
  
  public Priority getPriority() {
    return priority;
  }
  
  public LogProcessor setPriority(Priority priority) {
    this.priority = priority;
    return this;
  }
  
}
