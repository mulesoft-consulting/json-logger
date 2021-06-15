package org.mule.extension.jsonlogger.internal;

import org.mule.extension.jsonlogger.api.pojos.LoggerConfig;
import org.mule.extension.jsonlogger.internal.singleton.ConfigsSingleton;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.RefName;

import javax.inject.Inject;

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple
 * operations since they represent something core from the extension.
 */
@Operations(JsonloggerOperations.class)
public class JsonloggerConfiguration extends LoggerConfig implements Initialisable, Disposable {
  
  @Inject
  ConfigsSingleton configsSingleton;
  
  @RefName
  private String configName;
  
  public String getConfigName() {
    return configName;
  }
  
  @Override
  public void initialise() {
    configsSingleton.addConfig(configName, this);
  }
  
  @Override
  public void dispose() {
  }
}
