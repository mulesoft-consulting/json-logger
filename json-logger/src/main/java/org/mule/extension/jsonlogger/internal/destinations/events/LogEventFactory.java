package org.mule.extension.jsonlogger.internal.destinations.events;

import com.lmax.disruptor.EventFactory;

public class LogEventFactory implements EventFactory<LogEvent> {

    public LogEvent newInstance() {
        return new LogEvent();
    }
}
