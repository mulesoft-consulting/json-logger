package org.mule.modules.jsonloggermodule.config;

import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Required;
import org.mule.api.annotations.components.Configuration;
import org.mule.api.annotations.display.Placement;
import org.mule.api.annotations.param.Default;

@Configuration(configElementName = "json-logger-with-mq-config", friendlyName = "Logger with Anypoint MQ Configuration")
public class JsonLoggerWithMQConfig extends AbstractJsonLoggerConfig {
	
	@Configurable
	@Placement(group="Anypoint MQ Configuration")
	@Default(value = "https://mq-us-east-1.anypoint.mulesoft.com/api/v1")
	private String mqEndpoint;
	
	@Configurable
	@Placement(group="Anypoint MQ Configuration")
	@Required
	private String clientAppId;
	
	@Configurable
	@Placement(group="Anypoint MQ Configuration")
	@Required
	private String clientSecret;
	
	@Configurable
	@Placement(group="Anypoint MQ Configuration")
	@Required
	private String destination;

	public JsonLoggerWithMQConfig () throws Exception{
		setIsMQ(true);
	}

	public String getMqEndpoint() {
		return mqEndpoint;
	}

	public void setMqEndpoint(String mqEndpoint) {
		this.mqEndpoint = mqEndpoint;
	}

	public String getClientAppId() {
		return clientAppId;
	}

	public void setClientAppId(String clientAppId) {
		this.clientAppId = clientAppId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}
}