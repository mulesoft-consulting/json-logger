package org.mule.extension.jsonlogger.internal;

import static org.mule.runtime.api.el.BindingContext.builder;
import static org.mule.runtime.api.meta.ExpressionSupport.*;
import static org.mule.runtime.api.metadata.DataType.NUMBER;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.core.internal.el.mvel.MessageVariableResolverFactory.PAYLOAD;
import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.beanutils.BeanUtils;
import org.mule.extension.jsonlogger.pojos.LoggerProcessor;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.transformation.TransformationService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.*;
import org.mule.runtime.extension.api.annotation.values.OfValues;
import org.mule.runtime.extension.api.runtime.operation.FlowListener;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.parameter.CorrelationInfo;
import org.mule.weave.v2.model.values.TypeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class JsonloggerOperations implements Initialisable {

    /**
     * Create the SLF4J logger
     * jsonLogger: JSON output log
     * log: Connector internal log
     */
    private static final Logger jsonLogger = LoggerFactory.getLogger("org.mule.extension.jsonlogger.JsonLogger");
//    private static final Logger payloadLogger = LoggerFactory.getLogger("org.mule.extension.jsonlogger.PayloadLogger");
//    private static final Logger log = LoggerFactory.getLogger("org.mule.extension.jsonlogger.JsonLoggerExtension");

    // JSON Object Mapper
    private static final ObjectMapper om = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @Inject
    TransformationService transformationService;

    /**
     * Log a new entry
     */
    @MediaType(value = ANY, strict = false)
    public void logger(@ParameterGroup(name = "Logger") @Expression(value = NOT_SUPPORTED) LoggerProcessor loggerJson,
                       CorrelationInfo correlationInfo,
                       ComponentLocation location,
                       @Config JsonloggerConfiguration config) {

//        System.out.println("EventId: " + correlationInfo.getEventId());
//        System.out.println("CorrelationId: " + correlationInfo.getCorrelationId());
//        System.out.println("Location: " + location.getLocation());
        Long initialTimestamp = 0L;
        try {
            // Add cache entry for initial timestamp based on unique EventId
            initialTimestamp = config.getCachedTimerTimestamp(correlationInfo.getEventId());
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        //TODO: System.out.println("Location: " + location.getLocation());

        // Calculate elapsed time based on cached timestamp
        Long elapsed = System.currentTimeMillis() - initialTimestamp;

        // Define JSON output formatting
        ObjectWriter ow = (config.getJsonOutput().isPrettyPrint()) ? om.writer().withDefaultPrettyPrinter() : om.writer();
        String logLine;

        try {
            JsonNode nodeLoggerJson = om.valueToTree(loggerJson);
            JsonNode nodeConfigJson = om.valueToTree(config.getGlobalSettings());
            // Merge contents of loggerJson and globalSettings inside the config
            ObjectNode mergedLogger = (ObjectNode) merge(nodeLoggerJson, nodeConfigJson);

            /* Adding additional metadata
                - elapsed
                - threadName
                - locationInfo
             */
            if (elapsed != null) {
                mergedLogger.put("elapsed", Long.toString(elapsed));
            }
            mergedLogger.put("threadName", Thread.currentThread().getName());
            Map<String, String> locationInfo = new HashMap<String, String>();
            locationInfo.put("location", location.getLocation());
            locationInfo.put("rootContainer", location.getRootContainerName());
            locationInfo.put("component", location.getComponentIdentifier().toString());
            locationInfo.put("fileName", location.getFileName().orElse(""));
            locationInfo.put("lineInFile", String.valueOf(location.getLineInFile().orElse(null)));
            mergedLogger.putPOJO("locationInfo", locationInfo);

            /** [START] Print payload logic **/
            // Override connector config with environment variable: json.logger.global.disablePayloadLogging
            String disablePayloadLogging = System.getProperty("json.logger.global.disablePayloadLogging");
            if (disablePayloadLogging != null) {
                config.getJsonOutput().setDisablePayloadLogging(Boolean.valueOf(disablePayloadLogging));
            }
            if (!config.getJsonOutput().isDisablePayloadLogging()) {
                if (loggerJson.getLogPayload() != null) {
                    System.out.println(">>>>>>>>>> SHOULD LOG PAYLOAD: " + loggerJson.getLogPayload().getPayload());
                } else {
                    System.out.println(">>>>>>>>>> DON'T LOG PAYLOAD");
                }
                if (loggerJson.getLogAttributes() != null) {
                    System.out.println(">>>>>>>>>> SHOULD LOG ATTRIBUTES: " + loggerJson.getLogAttributes().getAttributes());
                } else {
                    System.out.println(">>>>>>>>>> DON'T LOG ATTRIBUTES");
                }
            }
            /** [END] Print payload logic **/

            logLine = ow.writeValueAsString(mergedLogger);

            doLog(loggerJson.getPriority().toString(), logLine);
            //TODO: add delayed resolution based on checkboxes

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Logs a line through the logging backend
     *
     * @param priority the severity
     * @param logLine  the line to log
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

    @Override
    public void initialise() throws InitialisationException {

    }

    private static JsonNode merge(JsonNode mainNode, JsonNode updateNode) {

        Iterator<String> fieldNames = updateNode.fieldNames();

        while (fieldNames.hasNext()) {
            String updatedFieldName = fieldNames.next();
            JsonNode valueToBeUpdated = mainNode.get(updatedFieldName);
            JsonNode updatedValue = updateNode.get(updatedFieldName);

            // If the node is an @ArrayNode
            if (valueToBeUpdated != null && valueToBeUpdated.isArray() &&
                    updatedValue.isArray()) {
                // running a loop for all elements of the updated ArrayNode
                for (int i = 0; i < updatedValue.size(); i++) {
                    JsonNode updatedChildNode = updatedValue.get(i);
                    // Create a new Node in the node that should be updated, if there was no corresponding node in it
                    // Use-case - where the updateNode will have a new element in its Array
                    if (valueToBeUpdated.size() <= i) {
                        ((ArrayNode) valueToBeUpdated).add(updatedChildNode);
                    }
                    // getting reference for the node to be updated
                    JsonNode childNodeToBeUpdated = valueToBeUpdated.get(i);
                    merge(childNodeToBeUpdated, updatedChildNode);
                }
                // if the Node is an @ObjectNode
            } else if (valueToBeUpdated != null && valueToBeUpdated.isObject()) {
                merge(valueToBeUpdated, updatedValue);
            } else {
                if (mainNode instanceof ObjectNode) {
                    ((ObjectNode) mainNode).replace(updatedFieldName, updatedValue);
                }
            }
        }
        return mainNode;
    }

    private String getMessageAsString(TypedValue payload) {
        return (String) transformationService.transform(Message.builder().payload(payload).build(), STRING).getPayload()
                .getValue();
    }
}
