package org.mule.extension.jsonlogger.config;

public enum TracePoint {
  
  START("START"),
  BEFORE_TRANSFORM("BEFORE_TRANSFORM"),
  AFTER_TRANSFORM("AFTER_TRANSFORM"),
  BEFORE_REQUEST("BEFORE_REQUEST"),
  AFTER_REQUEST("AFTER_REQUEST"),
  FLOW("FLOW"),
  END("END"),
  EXCEPTION("EXCEPTION");
  private final String value;
  
  TracePoint(String value) {
    this.value = value;
  }
  
  @Override
  public String toString() {
    return this.value;
  }
}
