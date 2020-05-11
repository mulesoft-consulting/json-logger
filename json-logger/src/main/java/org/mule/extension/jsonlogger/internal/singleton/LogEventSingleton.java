package org.mule.extension.jsonlogger.internal.singleton;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import org.mule.extension.jsonlogger.internal.destinations.Destination;
import org.mule.extension.jsonlogger.internal.destinations.events.LogEvent;
import org.mule.extension.jsonlogger.internal.destinations.events.LogEventHandler;

import javax.inject.Inject;

public class LogEventSingleton {

    @Inject
    ExternalDestinationsSingleton externalDestinations;

    // Specify the size of the ring buffer, must be power of 2.
    int bufferSize = 1024;

    // Construct the Disruptor
    private Disruptor<LogEvent> disruptor;
    private RingBuffer<LogEvent> ringBuffer;

    public static void translate(LogEvent logEvent, long sequence, String correlationId, String log, String configName)
    {
        logEvent.setLogEvent(correlationId, log, configName);
    }

//    @Override
    public void init() {
        // Check if there are external destinations configured
        if (disruptor == null) {
            // Init disruptor ring buffer
            this.disruptor  = new Disruptor<>(LogEvent::new, bufferSize, DaemonThreadFactory.INSTANCE);

            // Connect the handler with initialized list of destinations
            disruptor.handleEventsWith(new LogEventHandler(this.externalDestinations.getDestinations()));

            // Start the Disruptor, starts all threads running
            this.disruptor.start();

            // Get the ring buffer from the Disruptor to be used for publishing.
            this.ringBuffer = disruptor.getRingBuffer();
        }
    } //TODO: Figure out a dispose task and a cleanup task

    public void publishToExternalDestination(String correlationId, String log, String configName) {
        if (disruptor == null) {
            //TODO: How will this work with multiple configs?
            init();
        }
        System.out.println("Publishing event to ringBuffer for destination type: " + externalDestinations.getDestination(configName).getSelectedDestinationType());
        ringBuffer.publishEvent(LogEventSingleton::translate, correlationId, log, configName);
    }

}
