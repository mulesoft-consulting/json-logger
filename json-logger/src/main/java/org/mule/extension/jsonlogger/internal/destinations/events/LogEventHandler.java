package org.mule.extension.jsonlogger.internal.destinations.events;

import com.lmax.disruptor.EventHandler;
import org.mule.extension.jsonlogger.internal.destinations.Destination;
import org.mule.extension.jsonlogger.internal.singleton.LogEventSingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogEventHandler implements EventHandler<LogEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogEventHandler.class);

    private Map<String,List<String>> aggregatedLogsPerConfig = new HashMap<String, List<String>>();
    private Map<String, Destination> destinations = new HashMap<String, Destination>();

    public LogEventHandler (Map<String, Destination> destinations) {
        this.destinations = destinations;
    }

    public void onEvent(LogEvent logEvent, long sequence, boolean endOfBatch) {
        LOGGER.debug("Event Log received with correlationId: " + logEvent.getCorrelationId());
        if (aggregatedLogsPerConfig.get(logEvent.getConfigName()) == null){
            List<String> aggregatedLogs = new ArrayList<>();
            aggregatedLogs.add(logEvent.getLog());
            aggregatedLogsPerConfig.put(logEvent.getConfigName(), aggregatedLogs);
        } else {
            aggregatedLogsPerConfig.get(logEvent.getConfigName()).add(logEvent.getLog());
        }
        if (aggregatedLogsPerConfig.get(logEvent.getConfigName()).size() >= destinations.get(logEvent.getConfigName()).getMaxBatchSize()) {
            LOGGER.debug("Max batch size of " + destinations.get(logEvent.getConfigName()).getMaxBatchSize() + " reached for Config: " + logEvent.getConfigName() + ". Flushing logs...");
            flushLogs(logEvent.getConfigName());
        }
        if (endOfBatch) {
            LOGGER.debug("End of batch reached. Flushing all config logs...");
            flushAllLogs();
        }
    }

    private void flushLogs(String configName) {
        LOGGER.debug("Sending " + aggregatedLogsPerConfig.get(configName).size()+ " logs to external destination: " + this.destinations.get(configName).getSelectedDestinationType());
        try {
            this.destinations.get(configName).sendToExternalDestination(aggregatedLogsPerConfig.get(configName).toString());
        } catch (Exception e) {
            LOGGER.error("Error flushing aggregated logs: " + e.getMessage());
            e.printStackTrace();
        }
        aggregatedLogsPerConfig.get(configName).clear();
    }

    public void flushAllLogs() {
        try {
            for (String configName : aggregatedLogsPerConfig.keySet()) {
                if (aggregatedLogsPerConfig.get(configName).size() > 0) {
                    flushLogs(configName);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error flushing all aggregated logs: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
