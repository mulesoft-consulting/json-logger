package org.mule.extension.jsonlogger.internal.destinations;

import com.mule.extensions.amqp.api.message.AmqpMessageBuilder;
import com.mule.extensions.amqp.api.message.AmqpProperties;
import org.mule.extension.jsonlogger.api.pojos.Priority;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.mule.runtime.api.metadata.DataType.JSON_STRING;

public class AMQPDestination implements Destination {

    private static final Logger LOGGER = LoggerFactory.getLogger(AMQPDestination.class);

    @Inject
    ExtensionsClient extensionsClient;

    @Parameter
    @Optional
    @ConfigReference(namespace = "AMQP", name = "CONFIG")
    @DisplayName("Configuration Ref")
    private String amqpConfigurationRef;

    @Parameter
    @Optional
    @Summary("Name of the target exchange destination (e.g. logger-exchange)")
    @DisplayName("Exchange Destination")
    private String exchangeDestination;

    @Parameter
    @Optional
    @NullSafe
    @Summary("Indicate which log categories should be send (e.g. [\"my.category\",\"another.category\"]). If empty, all will be send.")
    @DisplayName("Log Categories")
    private ArrayList<String> logCategories;

    @Parameter
    @Optional(defaultValue = "25")
    @Summary("Indicate max quantity of logs entries to be send to the external destination")
    @DisplayName("Max Batch Size")
    private int maxBatchSize;

    @Override
    public int getMaxBatchSize() {
        return this.maxBatchSize;
    }

    @Override
    public String getSelectedDestinationType() {
        return "AMQP";
    }

    @Override
    public ArrayList<String> getSupportedCategories() {
        return logCategories;
    }

    @Override
    public void sendToExternalDestination(String finalLog) {
        try {
            OperationParameters parameters = DefaultOperationParameters.builder().configName(this.amqpConfigurationRef)
                    .addParameter("exchangeName", this.exchangeDestination)
                    .addParameter("messageBuilder", AmqpMessageBuilder.class, DefaultOperationParameters.builder()
                            .addParameter("body", new TypedValue<>(finalLog, JSON_STRING))
                            .addParameter("properties", new AmqpProperties()))
                    .build();
            extensionsClient.executeAsync("AMQP", "publish", parameters);
        } catch (Exception e) {
            LOGGER.error("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void initialise() {

    }

    @Override
    public void dispose() {

    }
}
