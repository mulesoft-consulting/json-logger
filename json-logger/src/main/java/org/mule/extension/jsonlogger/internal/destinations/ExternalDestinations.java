package org.mule.extension.jsonlogger.internal.destinations;


import org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.reference.ConfigReference;

@ExclusiveOptionals
public final class ExternalDestinations {

    public ExternalDestinations () {};

    @Parameter
    @Placement(order = 1, tab = "Destinations")
    @Optional
    @ConfigReference(namespace = "ANYPOINT-MQ", name = "CONFIG")
    private String anypointMQConfigurationRef;

    @Parameter
    @Placement(order = 1, tab = "Destinations")
    @Optional
    @ConfigReference(namespace = "JMS", name = "CONFIG")
    private String jmsConfigurationRef;

    @Parameter
    @Placement(order = 1, tab = "Destinations")
    @Optional
    @ConfigReference(namespace = "AMQP", name = "CONFIG")
    private String amqpConfigurationRef;

    private String selectedConfiguration = null;

    public String getAnypointMQConfigurationRef() {
        return anypointMQConfigurationRef;
    }

    public void setAnypointMQConfigurationRef(String anypointMQConfigurationRef) {
        this.anypointMQConfigurationRef = anypointMQConfigurationRef;
        selectedConfiguration = "AMQ";
    }

    public String getJmsConfigurationRef() {
        return jmsConfigurationRef;
    }

    public void setJmsConfigurationRef(String jmsConfigurationRef) {
        this.jmsConfigurationRef = jmsConfigurationRef;
        selectedConfiguration = "JMS";
    }

    public String getAmqpConfigurationRef() { return amqpConfigurationRef; }

    public void setAmqpConfigurationRef(String amqpConfigurationRef) {
        this.amqpConfigurationRef = amqpConfigurationRef;
        selectedConfiguration = "AMQP";
    }

    public String getSelectedConfiguration() {
        return selectedConfiguration;
    }

}