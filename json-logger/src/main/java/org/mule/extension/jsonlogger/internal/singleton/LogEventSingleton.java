package org.mule.extension.jsonlogger.internal.singleton;

import com.lmax.disruptor.LiteTimeoutBlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import org.mule.extension.jsonlogger.internal.destinations.Destination;
import org.mule.extension.jsonlogger.internal.destinations.events.LogEvent;
import org.mule.extension.jsonlogger.internal.destinations.events.LogEventHandler;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class LogEventSingleton implements Initialisable, Disposable {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogEventSingleton.class);

    // Specify the size of the ring buffer, must be power of 2
    private final Integer BUFFER_SIZE = Integer.valueOf(System.getProperty("json.logger.destinations.buffersize", "1024"));
    // Specify the event wait timeout in milliseconds before dropping the events
    private final Integer WAIT_TIMEOUT = Integer.valueOf(System.getProperty("json.logger.destinations.waittimeout", "100"));

    @Inject
    ConfigsSingleton configs;

    private HashMap<String, Destination> destinations = new HashMap<String, Destination>();

    // Construct the Disruptor
    private Disruptor<LogEvent> disruptor;
    private RingBuffer<LogEvent> ringBuffer;
    private LogEventHandler logEventHandler;

    public static void translate(LogEvent logEvent, long sequence, String correlationId, String log, String configName) {
        logEvent.setLogEvent(correlationId, log, configName);
    }

    public void publishToExternalDestination(String correlationId, String finalLog, String configName) {
        LOGGER.debug("Publishing event to ringBuffer for destination type: " + this.destinations.get(configName).getSelectedDestinationType());
        ringBuffer.publishEvent(LogEventSingleton::translate, correlationId, finalLog, configName);
    }

    @Override
    public void initialise() throws InitialisationException {
        LOGGER.debug("Init LogEventSingleton...");

        // Define waitStrategy: LiteTimeoutBlockingWaitStrategy avoids "blocking wait" but may cause LogEvent loss
        WaitStrategy waitStrategy = new LiteTimeoutBlockingWaitStrategy(WAIT_TIMEOUT, TimeUnit.MILLISECONDS);

        // Init disruptor ring buffer
        this.disruptor  = new Disruptor<>(LogEvent::new, BUFFER_SIZE, DaemonThreadFactory.INSTANCE, ProducerType.SINGLE, waitStrategy);

        // Load external destinations
        configs.getConfigs().forEach(
                (configName, config) -> this.destinations.put(configName, config.getExternalDestination())
        );
        this.logEventHandler = new LogEventHandler(this.destinations);
        // Connect the handler with initialized list of destinations
        disruptor.handleEventsWith(logEventHandler);

        // Start the Disruptor, starts all threads running
        this.disruptor.start();

        // Get the ring buffer from the Disruptor to be used for publishing.
        this.ringBuffer = disruptor.getRingBuffer();
    }

    @Override
    public void dispose() {
        this.disruptor.shutdown();
        this.logEventHandler.flushAllLogs();
    }
}
