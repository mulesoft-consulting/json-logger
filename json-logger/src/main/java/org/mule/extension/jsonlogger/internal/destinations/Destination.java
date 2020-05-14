package org.mule.extension.jsonlogger.internal.destinations;

import org.mule.runtime.extension.api.client.ExtensionsClient;

import java.util.ArrayList;
import java.util.Map;

public interface Destination {

    public String getSelectedDestinationType();

    public ArrayList<String> getSupportedCategories();

    public int getMaxBatchSize();

    public void sendToExternalDestination(String finalLog);

    public void initialise();

    public void dispose();
}
