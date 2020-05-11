package org.mule.extension.jsonlogger.internal.destinations.events;

import com.lmax.disruptor.EventHandler;
import org.mule.extension.jsonlogger.internal.destinations.Destination;
import org.mule.extension.jsonlogger.internal.singleton.ExternalDestinationsSingleton;
import org.mule.extension.jsonlogger.internal.singleton.ObjectMapperSingleton;
import org.mule.runtime.extension.api.client.ExtensionsClient;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogEventHandler implements EventHandler<LogEvent> {

    //private List<String> aggregatedLogs = new ArrayList<>();
    private Map<String,List<String>> aggregatedLogsPerConfig = new HashMap<String, List<String>>();
    private Map<String, Destination> destinations = new HashMap<String, Destination>();

    public LogEventHandler (Map<String, Destination> destinations) {
        System.out.println("Setting LogEventHandler destionations... should only happen ONCE!");
        this.destinations = destinations;
    }
    //TODO: FUCK!!! Buffer batch is diff from eventLog batch....
    //TODO: Consider dropping logs if ringbuffer is full to avoid impacting app perf
    public void onEvent(LogEvent logEvent, long sequence, boolean endOfBatch) {
        System.out.println("Event Log received with id: " + logEvent.getCorrelationId());
        //TODO: validate if buffer can be enhanced with timeouts or something triggered on flowListener.onComplete
        //TODO: the size value should be inherited from the config and called "maxBatchSize" (as it will use endOfBatch from ringbuffer as well)
        if (aggregatedLogsPerConfig.get(logEvent.getConfigName()) == null){
            List<String> aggregatedLogs = new ArrayList<>();
            aggregatedLogs.add(logEvent.getLog());
            aggregatedLogsPerConfig.put(logEvent.getConfigName(), aggregatedLogs);
        } else {
            aggregatedLogsPerConfig.get(logEvent.getConfigName()).add(logEvent.getLog());
        }
        if (aggregatedLogsPerConfig.get(logEvent.getConfigName()).size() >= 100) {
            System.out.println("Max batch size reached for Config: " + logEvent.getConfigName() + ". Flushing logs...");
            flushLogs(logEvent.getConfigName());
        }
        if (endOfBatch) {
            System.out.println("End of batch reached. Flushing all config logs...");
            flushAllLogs();
        }
    }

    private void flushLogs(String configName) {
        System.out.println("Sending " + aggregatedLogsPerConfig.get(configName).size()+ " logs to external destination: " + this.destinations.get(configName).getSelectedDestinationType());
        try {
            //TODO: Validate if extensionsClient can be injected on each Destination instead of passing it around - URGENT
            this.destinations.get(configName).sendToExternalDestination(aggregatedLogsPerConfig.get(configName).toString());
        } catch (Exception e) {
            System.out.println("Error flushing aggregated logs: " + e.getMessage());
            e.printStackTrace();
        }
        aggregatedLogsPerConfig.get(configName).clear();
    }

    private void flushAllLogs() {
        try {
            for (String configName : aggregatedLogsPerConfig.keySet()) {
                if (aggregatedLogsPerConfig.get(configName).size() > 0) {
                    flushLogs(configName);
                }
            }
        } catch (Exception e) {
            System.out.println("Error flushing all aggregated logs: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
