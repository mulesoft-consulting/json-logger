package org.mule.extension.jsonlogger.internal.destinations;

import org.mule.runtime.extension.api.client.ExtensionsClient;

import java.util.ArrayList;
import java.util.Map;

public interface Destination {

    public String getSelectedDestinationType();

    public ArrayList<String> getSupportedCategories();

    public void sendToExternalDestination(ExtensionsClient client, String finalLog, String correlationId);
}
