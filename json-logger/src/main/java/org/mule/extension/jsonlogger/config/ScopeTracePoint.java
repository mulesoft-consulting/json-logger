package org.mule.extension.jsonlogger.config;

public enum ScopeTracePoint {
  
  DATA_TRANSFORM_SCOPE("DATA_TRANSFORM_SCOPE"),
  OUTBOUND_REQUEST_SCOPE("OUTBOUND_REQUEST_SCOPE"),
  FLOW_LOGIC_SCOPE("FLOW_LOGIC_SCOPE");
  private final String value;
  
  ScopeTracePoint(String value) {
    this.value = value;
  }
  
  @Override
  public String toString() {
    return this.value;
  }
}