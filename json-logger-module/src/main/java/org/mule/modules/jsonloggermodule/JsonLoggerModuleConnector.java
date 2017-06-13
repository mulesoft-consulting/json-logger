package org.mule.modules.jsonloggermodule;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.beanutils.BeanUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mule.api.MuleEvent;
import org.mule.api.annotations.Config;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Literal;
import org.mule.api.annotations.param.Optional;
import org.mule.api.expression.ExpressionManager;
import org.mule.modules.jsonloggermodule.config.ConnectorConfig;
import org.mule.modules.pojos.LoggerProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

@Connector(name="json-logger", friendlyName="JsonLoggerModule")
public class JsonLoggerModuleConnector {

    @Config
    ConnectorConfig config;
    
    @Inject
    ExpressionManager expressionManager;

    /** 
	 * Create the SLF4J logger
	 */
    private static final Logger logger = LoggerFactory.getLogger("org.mule.modules.jsonloggermodule.JsonLoggerModuleConnector");
    
    private static final ObjectMapper om = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        
	@PostConstruct 
	public void initialize() {
	}
	
    /**
     * Log a new entry
     */
    @Processor
    public void logger(
    		@Literal @Default(value="#[flowVars['timerVariable']]") String timerVariable, 
    		LoggerProcessor loggerJson, 
    		MuleEvent event) {
    	
    	// Calculate elapsed time based on timeVariable. Default flowVars['timerVariable']
		if (timerVariable == null) timerVariable = "#[flowVars['timerVariable']]";
		
		Long elapsed = null;
		if (!expressionManager.parse(timerVariable, event).equals("null")) {
			Long current = System.currentTimeMillis();
			elapsed = current - Long.parseLong(expressionManager.parse(timerVariable, event));
		} else {
			expressionManager.enrich(timerVariable, event, System.currentTimeMillis()); // Set the variable value to the current timestamp for the next elapsed calculation
		}

		// Define JSON output formatting 
		ObjectWriter ow = null;
		if (config.getPrettyPrint() == true) {
			ow = om.writer().withDefaultPrettyPrinter();
		} else {
			ow = om.writer();
		}
		
		String logLine;
		
		// Copy default values from config into processor object
		try {
			
			if (elapsed != null) {
				loggerJson.setElapsed(Long.toString(elapsed));
			}
			
			BeanUtils.copyProperties(loggerJson, config);
			
			BeanUtils.describe(loggerJson).forEach((k,v) ->{ 
				if (v != null && v.startsWith("#[")) {
					try {
						//System.out.println("Processing key: " + k);
						BeanUtils.setProperty(loggerJson, k, expressionManager.parse(v, event));
					} catch (Exception e) {
						System.out.println("Failed parsing expression for key: " + k);
					}
				}
			});
				
			logLine = ow.writeValueAsString(loggerJson);
			
			doLog(loggerJson.getPriority().toString(), logLine);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    /**
	 * Logs a line through the logging backend
	 * @param severity the severity
	 * @param logLine the line to log
	 */
	private void doLog(String priority, String logLine) {
		switch (priority) {
		case "TRACE":
			logger.trace(logLine);
			break;
		case "DEBUG":
			logger.debug(logLine);
			break;
		case "INFO":
			logger.info(logLine);
			break;
		case "WARN":
			logger.warn(logLine);
			break;
		case "ERROR":
			logger.error(logLine);
			break;
		}
	}
    
    public ConnectorConfig getConfig() {
        return config;
    }

    public void setConfig(ConnectorConfig config) {
        this.config = config;
    }
    
    public void setExpressionManager(ExpressionManager expressionManager) {
    	this.expressionManager = expressionManager;
    }

}