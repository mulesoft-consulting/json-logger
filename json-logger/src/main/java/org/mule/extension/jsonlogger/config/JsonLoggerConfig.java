package org.mule.extension.jsonlogger.config;

import org.mule.extension.jsonlogger.JsonLoggerOperations;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;

@Configuration(name = "config")
@Operations({JsonLoggerOperations.class})
public class JsonLoggerConfig {
  
  @Parameter
  @Optional(defaultValue = "")
  @Expression(NOT_SUPPORTED)
  String applicationName;
  
  @Parameter
  @Optional(defaultValue = "")
  @Expression(NOT_SUPPORTED)
  String applicationVersion;
  
  @Parameter
  @Optional(defaultValue = "")
  @Expression(NOT_SUPPORTED)
  String environment;
  
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  String disabledFields;
  
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  String contentFieldsDataMasking;
  
  @Parameter
  @Optional(defaultValue = "true")
  @Expression(NOT_SUPPORTED)
  boolean prettyPrint;
  
  @Parameter
  @Optional(defaultValue = "true")
  @Expression(NOT_SUPPORTED)
  boolean logLocationInfo;
  
  @Parameter
  @Optional(defaultValue = "true")
  @Expression(NOT_SUPPORTED)
  boolean parseContentFieldsInJsonOutput;
  
  public String getApplicationName() {
    return applicationName;
  }
  
  public void setApplicationName(String applicationName) {
    this.applicationName = applicationName;
  }
  
  public String getApplicationVersion() {
    return applicationVersion;
  }
  
  public void setApplicationVersion(String applicationVersion) {
    this.applicationVersion = applicationVersion;
  }
  
  public String getEnvironment() {
    return environment;
  }
  
  public void setEnvironment(String environment) {
    this.environment = environment;
  }
  
  public boolean isPrettyPrint() {
    return prettyPrint;
  }
  
  public void setPrettyPrint(boolean prettyPrint) {
    this.prettyPrint = prettyPrint;
  }
  
  public boolean isLogLocationInfo() {
    return logLocationInfo;
  }
  
  public void setLogLocationInfo(boolean logLocationInfo) {
    this.logLocationInfo = logLocationInfo;
  }
  
  public boolean isParseContentFieldsInJsonOutput() {
    return parseContentFieldsInJsonOutput;
  }
  
  public void setParseContentFieldsInJsonOutput(boolean parseContentFieldsInJsonOutput) {
    this.parseContentFieldsInJsonOutput = parseContentFieldsInJsonOutput;
  }
  
  public String getDisabledFields() {
    return disabledFields;
  }
  
  public void setDisabledFields(String disabledFields) {
    this.disabledFields = disabledFields;
  }
  
  public String getContentFieldsDataMasking() {
    return contentFieldsDataMasking;
  }
  
  public void setContentFieldsDataMasking(String contentFieldsDataMasking) {
    this.contentFieldsDataMasking = contentFieldsDataMasking;
  }
  
  @Override
  public String toString() {
    return "{" +
      "applicationName='" + applicationName + '\'' +
      ", applicationVersion='" + applicationVersion + '\'' +
      ", environment='" + environment + '\'' +
      ", disabledFields='" + disabledFields + '\'' +
      ", contentFieldsDataMasking='" + contentFieldsDataMasking + '\'' +
      ", prettyPrint=" + prettyPrint +
      ", logLocationInfo=" + logLocationInfo +
      ", parseContentFieldsInJsonOutput=" + parseContentFieldsInJsonOutput +
      '}';
  }
}
