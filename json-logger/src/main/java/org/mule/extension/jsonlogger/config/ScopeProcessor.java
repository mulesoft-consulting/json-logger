package org.mule.extension.jsonlogger.config;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

public class ScopeProcessor {
  
  @Parameter
  @Optional(defaultValue = "OUTBOUND_REQUEST_SCOPE")
  @Summary("Current processing stage")
  private ScopeTracePoint scopeTracePoint;
  
  @Parameter
  @Optional(defaultValue = "#[correlationId]")
  @Placement(tab = "Advanced")
  private String correlationId;
  
  @Parameter
  @Optional(defaultValue = "INFO")
  @Summary("Logger priority")
  private Priority priority;
  
  public ScopeTracePoint getScopeTracePoint() {
    return scopeTracePoint;
  }
  
  public void setScopeTracePoint(ScopeTracePoint scopeTracePoint) {
    this.scopeTracePoint = scopeTracePoint;
  }
  
  public String getCorrelationId() {
    return correlationId;
  }
  
  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }
  
  public Priority getPriority() {
    return priority;
  }
  
  public void setPriority(Priority priority) {
    this.priority = priority;
  }
}