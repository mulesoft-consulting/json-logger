package org.mule.extension.jsonlogger.internal.singleton;

import org.mule.extension.jsonlogger.internal.JsonloggerConfiguration;

import java.util.HashMap;
import java.util.Map;

public class ConfigsSingleton {

    private Map<String, JsonloggerConfiguration> configs = new HashMap<String, JsonloggerConfiguration>();

    public Map<String, JsonloggerConfiguration> getConfigs() {
        return configs;
    }

    public JsonloggerConfiguration getConfig(String configName) {
        return this.configs.get(configName);
    }

    public void addConfig(String configName, JsonloggerConfiguration config) {
        this.configs.put(configName, config);
    }

}
