package org.mule.modules.jsonloggermodule;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.commons.beanutils.BeanUtils;
import org.json.JSONException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.annotations.Config;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.lifecycle.Start;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Literal;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.registry.Registry;
import org.mule.api.store.ObjectStoreException;
import org.mule.modules.jsonloggermodule.config.AbstractJsonLoggerConfig;
import org.mule.modules.jsonloggermodule.config.JsonLoggerWithMQConfig;
import org.mule.modules.jsonloggermodule.utils.AnypointMQHelper;
import org.mule.modules.pojos.LoggerProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

@Connector(name="json-logger", friendlyName="JSON Logger")
public class JsonLoggerModuleConnector {

    @Config
    AbstractJsonLoggerConfig config;
    
    /**
     * Mule expression manager to resolve MEL expressions
     */
    @Inject
    ExpressionManager expressionManager;
    
    /**
     * Mule registry object required to perform store lookup.
     */
    @Inject
    private Registry registry;

    /**
     * Manages mule objects lifecycle
     */
    @Inject
    private MuleContext muleContext = null;

    /**
     * Temp definition for MQ in case it is required
     */
    private AnypointMQHelper amq;
    
    /** 
	 * Create the SLF4J logger
	 * jsonLogger: JSON output log
	 * log: Connector internal log 
	 */
    private static final Logger jsonLogger = LoggerFactory.getLogger("org.mule.modules.JsonLogger");
    private static final Logger log = LoggerFactory.getLogger("org.mule.modules.jsonloggermodule.JsonLoggerModuleConnector");
    
    private static final ObjectMapper om = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /**
     * Initializes and validates the configuration parameters. It retrieves an instance of object store from mule objectStoreManager.
     * @throws IOException 
     * @throws JSONException 
     * @throws ObjectStoreException 
     */
    @Start
	public void init() throws ObjectStoreException, JSONException, IOException {
		if (config.getIsMQ()) {
			JsonLoggerWithMQConfig amqConf = (JsonLoggerWithMQConfig) config;
			amq = new AnypointMQHelper(amqConf.getClientAppId(), amqConf.getClientSecret(), amqConf.getMqEndpoint(), amqConf.getDestination(), registry, muleContext);
		}
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
			loggerJson.setThreadName(Thread.currentThread().getName());
			
			BeanUtils.copyProperties(loggerJson, config);
			
			BeanUtils.describe(loggerJson).forEach((k,v) ->{ 
				if (v != null && v.startsWith("#[")) {
					try {
						log.debug("Processing key: " + k);
						BeanUtils.setProperty(loggerJson, k, expressionManager.parse(v, event));
					} catch (Exception e) {
						log.error("Failed parsing expression for key: " + k);
					}
				}
			});	
			logLine = ow.writeValueAsString(loggerJson);
						
			doLog(loggerJson.getPriority().toString(), logLine);
			
			if (config.getIsMQ()) {
				amq.send(logLine);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		/* Note: 
		 * This was added because when the payload before the JSON Logger is of type ByteArraySeekableStream
		 * (e.g. standard DW output object) and we pass #[payload] as the Message expression, then the stream 
		 * gets consumed but not reset which translates to an "empty stream" for the next processor.  
		 */
		if (event.getMessage().getPayload().getClass().getSimpleName().equals("ByteArraySeekableStream")) {
			log.debug("Payload is a ByteArraySeekableStream. Preemptively resetting the stream...");
			expressionManager.parse("#[payload.seek(0);]", event);
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
			jsonLogger.trace(logLine);
			break;
		case "DEBUG":
			jsonLogger.debug(logLine);
			break;
		case "INFO":
			jsonLogger.info(logLine);
			break;
		case "WARN":
			jsonLogger.warn(logLine);
			break;
		case "ERROR":
			jsonLogger.error(logLine);
			break;
		}
	}
		
    public AbstractJsonLoggerConfig getConfig() {
        return config;
    }

    public void setConfig(AbstractJsonLoggerConfig config) {
        this.config = config;
    }
    
    public void setExpressionManager(ExpressionManager expressionManager) {
    	this.expressionManager = expressionManager;
    }
    
    public void setRegistry(final Registry registry) {
        this.registry = registry;
    }

    public void setMuleContext(MuleContext context) {
        muleContext = context;
    }

}