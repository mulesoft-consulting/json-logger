package org.mule.extension.jsonlogger.internal.destinations;

import java.util.ArrayList;

public interface Destination {
  
  public String getSelectedDestinationType();
  
  public ArrayList<String> getSupportedCategories();
  
  public int getMaxBatchSize();
  
  public void sendToExternalDestination(String finalLog);
  
  public void initialise();
  
  public void dispose();
}
