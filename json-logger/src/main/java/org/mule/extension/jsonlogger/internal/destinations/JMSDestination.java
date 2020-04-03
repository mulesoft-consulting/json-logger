package org.mule.extension.jsonlogger.internal.destinations;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.reference.ConfigReference;

public class JMSDestination implements Destination {

    @Parameter
    @Placement(order = 1, tab = "Destinations")
    @Optional
    @ConfigReference(namespace = "JMS", name = "CONFIG")
    private String jmsConfigurationRef;

}
