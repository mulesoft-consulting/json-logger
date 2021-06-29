package org.mule.extension.jsonlogger.config;

public enum Priority {
  DEBUG("DEBUG"),
  TRACE("TRACE"),
  INFO("INFO"),
  WARN("WARN"),
  ERROR("ERROR");
  
  private final String value;
  
  Priority(String value) {
    this.value = value;
  }
  
  @Override
  public String toString() {
    return this.value;
  }
}
