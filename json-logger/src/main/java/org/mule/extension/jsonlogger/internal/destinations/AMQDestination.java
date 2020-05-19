package org.mule.extension.jsonlogger.internal.destinations;

import com.mulesoft.mq.restclient.api.*;
import com.mulesoft.mq.restclient.impl.OAuthCredentials;
import org.mule.extension.jsonlogger.internal.destinations.amq.client.MuleBasedAnypointMQClientFactory;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.client.HttpClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AMQDestination implements Destination {

    private static final Logger LOGGER = LoggerFactory.getLogger(AMQDestination.class);

    @Parameter
    @Optional
    @Summary("Name of the target queue or exchange destination (e.g. logger-queue, logger-exchange)")
    @DisplayName("Queue or Exchange Destination")
    private String queueOrExchangeDestination;

    /**
     * The region URL where the Queue resides. This URL can be obtained and configured from the Anypoint Platform &gt; MQ console.
     * Copy/paste the region URL into this field."
     */
    @Parameter
    @DisplayName("URL")
    @Example("https://mq-us-east-1.anypoint.mulesoft.com/api/v1")
    @Optional(defaultValue = "https://mq-us-east-1.anypoint.mulesoft.com/api/v1")
    @Summary("The region URL where the Queue resides. Obtain this URL from the Anypoint Platform > MQ")
    private String url;

    /**
     * In Anypoint Platform &gt; MQ &gt; Client Apps, click an app name (or create a new app) and
     * click Copy for the Client App ID field. Paste this value in the Studio Client App ID field
     */
    @Parameter
    @DisplayName("Client App ID")
    @Summary("The Client App ID to be used. Obtain this ID from Anypoint Platform > MQ > Client Apps")
    private String clientId;

    /**
     * In Anypoint Platform > MQ > Client Apps, click an app name (or create a new app) and
     * click Copy for the Client Secret field. Paste this value in the Studio Client Secret field.
     */
    @Parameter
    @DisplayName("Client Secret")
    @Password
    @Summary("The Client App Secret for the given Client App ID")
    private String clientSecret;

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

    @Inject
    protected HttpService httpService;

    @Inject
    protected SchedulerService schedulerService;

    private final String AMQ_HTTP_CLIENT = "amqHttpClient";
    private final String USER_AGENT_VERSION = "3.1.0"; // Version of the AMQ Connector code this logic is based of

    private HttpClientConfiguration httpClientConfiguration;
    private HttpClient httpClient;
    private AnypointMqClient amqClient;
    private DestinationLocator destinationLocator;
    private DestinationLocation location;

    @Override
    public String getSelectedDestinationType() {
        return "AMQ";
    }

    @Override
    public ArrayList<String> getSupportedCategories() {
        return logCategories;
    }

    @Override
    public void sendToExternalDestination(String finalLog) {

        try {
            // Send message
            MediaType mediaType = MediaType.parse("application/json; charset=UTF-8");
            AnypointMQMessage message = createMessage(finalLog, true, mediaType.toString(),
                    mediaType.getCharset(), null, new HashMap<>(), null, null);

            this.destinationLocator.getDestination(this.location)
                    .send(message)
                    .subscribe(new CourierObserver<MessageIdResult>() {
                        @Override
                        public void onSuccess(MessageIdResult result) {
                            LOGGER.debug("AMQ Message Id: " + result.getMessageId());
                        }

                        @Override
                        public void onError(Throwable e) {
                            String msg = String.format("Failed to publish message to destination '%s': %s", location, e.getMessage());
                            LOGGER.error(msg, e);
                        }
                    });
        } catch (Exception e) {
            LOGGER.error("Error sending message to AMQ: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static AnypointMQMessage createMessage(String messageBody, boolean sendContentType, String mediaType,
                                                   java.util.Optional<Charset> charset, String messageId, Map<String, String> properties,
                                                   java.util.Optional<Long> deliveryDelay, java.util.Optional<String> messageGroupId) {
        AnypointMQMessageBuilder messageBuilder = new AnypointMQMessageBuilder();
        messageBuilder.withBody(new ByteArrayInputStream(messageBody.getBytes()));

        String id = java.util.Optional.<Object>ofNullable(messageId).orElseGet(UUID::randomUUID).toString();
        messageBuilder.withMessageId(id);

        if (sendContentType) {
            messageBuilder.addProperty(AnypointMQMessage.Properties.AMQ_MESSAGE_CONTENT_TYPE, mediaType);
            charset.map(Object::toString)
                    .ifPresent(value -> messageBuilder.addProperty("MULE_ENCODING", value));
        }

        if (properties != null) {
            messageBuilder.withProperties(properties);
        }

        return messageBuilder.build();
    }

    public void initialise() {
        // Start HTTP Configuration
        Long startTimestamp = System.currentTimeMillis();
        this.httpClientConfiguration = new HttpClientConfiguration.Builder()
                .setName(AMQ_HTTP_CLIENT)
                .build();
        this.httpClient = httpService.getClientFactory().create(this.httpClientConfiguration);
        httpClient.start();

        // Start AMQ Client
        this.amqClient = new MuleBasedAnypointMQClientFactory(this.httpClient, schedulerService.ioScheduler())
                .createClient(url, new OAuthCredentials(clientId, clientSecret), USER_AGENT_VERSION);
        this.amqClient.init();

        // Locate AMQ destination
        this.destinationLocator = amqClient.createDestinationLocator();

        // Destination Location
        this.location = this.destinationLocator.getDestinationLocation(queueOrExchangeDestination);
    }

    public void dispose() {
        this.httpClient.stop();
        this.amqClient.dispose();
    }

}
