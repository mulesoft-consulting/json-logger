package org.mule.extension.jsonlogger.internal.singleton;

import org.mule.extension.jsonlogger.internal.destinations.Destination;
import org.mule.runtime.extension.api.client.ExtensionsClient;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class ExternalDestinationsSingleton {

    private Map<String, Destination> destinations = new HashMap<String, Destination>();

    public Map<String, Destination> getDestinations() {
        return destinations;
    }

    public Destination getDestination(String configName) {
        return this.destinations.get(configName);
    }

    public void addDestination(String configName, Destination destination) {
        this.destinations.put(configName, destination);
    }

}
