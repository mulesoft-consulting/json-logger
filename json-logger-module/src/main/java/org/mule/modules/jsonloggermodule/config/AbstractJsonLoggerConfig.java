package org.mule.modules.jsonloggermodule.config;

public abstract class AbstractJsonLoggerConfig extends org.mule.modules.pojos.LoggerConfig {

	private Boolean isMQ = false;
	
	public AbstractJsonLoggerConfig() {
	}

	public Boolean getIsMQ() {
		return isMQ;
	}

	public void setIsMQ(Boolean isMQ) {
		this.isMQ = isMQ;
	}

}
