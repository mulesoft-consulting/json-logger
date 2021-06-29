package org.mule.extension.jsonlogger.config;

import org.mule.extension.jsonlogger.JsonLoggerOperations;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;

@Configuration(name = "config")
@Operations({JsonLoggerOperations.class})
public class JsonLoggerConfig {
  
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  String contentFieldsDataMasking;
  
  @Parameter
  @Optional(defaultValue = "true")
  @Expression(NOT_SUPPORTED)
  boolean prettyPrint;
  
  
  @Parameter
  @Optional(defaultValue = "applicationName")
  @Summary("Name of the application")
  private String applicationName;
  
  @Parameter
  @Optional(defaultValue = "applicationVersion")
  @Summary("Version of the application")
  private String applicationVersion;
  
  @Parameter
  @Optional(defaultValue = "environment")
  private String environment;
  
  public String getApplicationName() {
    return applicationName;
  }
  
  public JsonLoggerConfig setApplicationName(String applicationName) {
    this.applicationName = applicationName;
    return this;
  }
  
  public String getApplicationVersion() {
    return applicationVersion;
  }
  
  public JsonLoggerConfig setApplicationVersion(String applicationVersion) {
    this.applicationVersion = applicationVersion;
    return this;
  }
  
  public String getEnvironment() {
    return environment;
  }
  
  public JsonLoggerConfig setEnvironment(String environment) {
    this.environment = environment;
    return this;
  }
  
  public boolean isPrettyPrint() {
    return prettyPrint;
  }
  
  public JsonLoggerConfig setPrettyPrint(boolean prettyPrint) {
    this.prettyPrint = prettyPrint;
    return this;
  }
  
  public String getContentFieldsDataMasking() {
    return contentFieldsDataMasking;
  }
  
  public JsonLoggerConfig setContentFieldsDataMasking(String contentFieldsDataMasking) {
    this.contentFieldsDataMasking = contentFieldsDataMasking;
    return this;
  }
}
