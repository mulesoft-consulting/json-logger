package org.mule.extension.jsonlogger.internal.destinations;

import org.mule.extension.jsonlogger.api.pojos.Priority;
import org.mule.extensions.jms.api.message.JmsMessageBuilder;
import org.mule.extensions.jms.api.message.JmsxProperties;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.annotation.param.reference.ConfigReference;
import org.mule.runtime.extension.api.client.DefaultOperationParameters;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.client.OperationParameters;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.mule.runtime.api.metadata.DataType.JSON_STRING;

public class JMSDestination implements Destination {

    @Inject
    ExtensionsClient extensionsClient;

    @Parameter
    @Optional
    @ConfigReference(namespace = "JMS", name = "CONFIG")
    @DisplayName("Configuration Ref")
    private String jmsConfigurationRef;

    @Parameter
    @Optional
    @Summary("Name of the target queue destination (e.g. logger-queue)")
    @DisplayName("Queue Destination")
    private String queueDestination;

    @Parameter
    @Optional
    @NullSafe
    @Summary("Indicate which log categories should be send (e.g. [\"my.category\",\"another.category\"]). If empty, all will be send.")
    @DisplayName("Log Categories")
    private ArrayList<String> logCategories;

    @Override
    public String getSelectedDestinationType() {
        return "JMS";
    }

    @Override
    public ArrayList<String> getSupportedCategories() {
        return logCategories;
    }

    @Override
    public void sendToExternalDestination(String finalLog) { //TODO: REMOVE CLIENT PARAM
        try {
            OperationParameters parameters = DefaultOperationParameters.builder().configName(this.jmsConfigurationRef)
                    .addParameter("destination", this.queueDestination)
                    .addParameter("messageBuilder", JmsMessageBuilder.class, DefaultOperationParameters.builder()
                            .addParameter("body", new TypedValue<>(finalLog, JSON_STRING))
                            .addParameter("jmsxProperties", new JmsxProperties())
                            .addParameter("properties", new HashMap<String, Object>()))
                    .build();
            extensionsClient.executeAsync("JMS", "publish", parameters);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
