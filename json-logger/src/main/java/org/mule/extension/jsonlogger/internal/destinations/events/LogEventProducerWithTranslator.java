package org.mule.extension.jsonlogger.internal.destinations.events;

import com.lmax.disruptor.EventTranslatorThreeArg;
import com.lmax.disruptor.EventTranslatorTwoArg;
import com.lmax.disruptor.RingBuffer;

public class LogEventProducerWithTranslator {

    private final RingBuffer<LogEvent> ringBuffer;

    public LogEventProducerWithTranslator(RingBuffer<LogEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    private static final EventTranslatorThreeArg<LogEvent, String, String, String> TRANSLATOR_THREE_ARG = new EventTranslatorThreeArg<LogEvent, String, String, String>() {
        public void translateTo(LogEvent logEvent, long sequence, String correlationId, String log, String configName) {
            logEvent.setLogEvent(correlationId, log, configName);
        }
    };

    public void onData(String correlationId, String log, String configName) {
        ringBuffer.publishEvent(TRANSLATOR_THREE_ARG, correlationId, log, configName);
    }
}
