package org.mule.custom.annotation.utils;

import java.util.Map;

import org.jsonschema2pojo.AbstractAnnotator;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Ignore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JFieldVar;
import org.mule.api.annotations.param.Default;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import static org.mule.runtime.api.meta.ExpressionSupport.*;

/**
 * Custom Devkit Annotator
 *
 */
public class CustomMuleAnnotator extends AbstractAnnotator {
	@Override
	  public void propertyField( JFieldVar field, JDefinedClass clazz, String propertyName, JsonNode propertyNode ) {
	    super.propertyField(field, clazz, propertyName, propertyNode);

	    ObjectMapper mapper = new ObjectMapper();
	    Map<String, Object> jsonMap = mapper.convertValue(propertyNode, Map.class);
	    Map<String, Object> devkitMap = (Map<String, Object>) jsonMap.get("devkit");
		Map<String, Object> sdkMap = (Map<String, Object>) jsonMap.get("sdk");

	    if (devkitMap != null) {
			System.out.println(">> devkitMap: " + devkitMap);

	    	if (devkitMap.get("placement") != null) {
		    	String placement = String.valueOf(devkitMap.get("placement"));
		    	field.annotate(org.mule.api.annotations.display.Placement.class).param("group", placement);
	    	}
	    	
	    	String defaultValue = String.valueOf(devkitMap.get("default"));
	    	field.annotate(Default.class).param("value", defaultValue);
	    	
	    	if (Boolean.TRUE.equals(devkitMap.get("isConfig"))) 
	    		field.annotate(Configurable.class);
	    	
	    	if (Boolean.TRUE.equals(devkitMap.get("isHidden")))
		    	field.annotate(Ignore.class);
	    }

		if (sdkMap != null) {
			System.out.println(">> sdkMap: " + sdkMap);

			//TODO: Evaluate this for the config
//			if (sdkMap.get("placement") != null) {
//				String placement = (String) sdkMap.get("placement");
//				field.annotate(Placement.class).param("group", placement);
//			}
			field.annotate(Parameter.class);
            if (sdkMap.get("default") != null) {
                String defaultValue = String.valueOf(sdkMap.get("default"));
                System.out.println(">> Found default: " + defaultValue);
                field.annotate(Optional.class).param("defaultValue", defaultValue);
            } else if (sdkMap.get("required") != null && Boolean.FALSE.equals(sdkMap.get("required"))){
                System.out.println(">> Field is required");
                field.annotate(Optional.class);
            }
            if (sdkMap.get("displayName") != null) {
                String displayNameValue = String.valueOf(sdkMap.get("displayName"));
                System.out.println(">> Found displayName: " + displayNameValue);
                field.annotate(DisplayName.class).param("value", displayNameValue);
            }
            if (sdkMap.get("summary") != null) {
                String summaryValue = String.valueOf(sdkMap.get("summary"));
                System.out.println(">> Found summary: " + summaryValue);
                field.annotate(Summary.class).param("value", summaryValue);
            }
            if (sdkMap.get("example") != null) {
                String exampleValue = String.valueOf(sdkMap.get("example"));
                System.out.println(">> Found example: " + exampleValue);
                field.annotate(Example.class).param("value", exampleValue);
            }
            if (sdkMap.get("isContent") != null) {
                System.out.println(">> Found isContent: " + String.valueOf(sdkMap.get("isContent")));
                if (Boolean.TRUE.equals(sdkMap.get("isContent"))) {
                    field.annotate(Content.class);
                }
            }
            if (sdkMap.get("isPrimaryContent") != null) {
                System.out.println(">> Found isPrimaryContent: " + String.valueOf(sdkMap.get("isPrimaryContent")));
                if (Boolean.TRUE.equals(sdkMap.get("isPrimaryContent"))) {
                    field.annotate(Content.class).param("primary", true);
                }
            }
            if (sdkMap.get("expressionSupport") != null) {
                String expressionSupportValue = String.valueOf(sdkMap.get("expressionSupport"));
                System.out.println(">> Found expressionSupport: " + expressionSupportValue);
                switch(expressionSupportValue)
                {
                    case "NOT_SUPPORTED" :
                        field.annotate(Expression.class).param("value", NOT_SUPPORTED);
                        break;
                    case "SUPPORTED" :
                        field.annotate(Expression.class).param("value", SUPPORTED);
                        break;
                    case "REQUIRED" :
                        field.annotate(Expression.class).param("value", REQUIRED);
                        break;
                    default :
                        throw new java.lang.IllegalArgumentException("expressionSupport value not supported");
                }
            }
            if (sdkMap.get("placement") != null) {
                Map<String, Object> placementMap = (Map<String, Object>) sdkMap.get("placement");
                if (placementMap.get("order") != null && placementMap.get("tab") != null) {
                    Integer orderValue = Integer.valueOf(String.valueOf(placementMap.get("order")));
                    System.out.println(">> Found placement.order: " + orderValue);
                    String tabValue = String.valueOf(placementMap.get("tab"));
                    System.out.println(">> Found placement.tab: " + tabValue);
                    field.annotate(Placement.class)
                            .param("order", orderValue)
                            .param("tab", tabValue);
                } else if (placementMap.get("order") != null) {
                    Integer orderValue = Integer.valueOf(String.valueOf(placementMap.get("order")));
                    System.out.println(">> Found placement.order: " + orderValue);
                    field.annotate(Placement.class).param("order", orderValue);
                } else if (placementMap.get("tab") != null) {
                    String tabValue = String.valueOf(placementMap.get("tab"));
                    System.out.println(">> Found placement.tab: " + tabValue);
                    field.annotate(Placement.class).param("tab", tabValue);
                }
            }
            if (sdkMap.get("parameterGroup") != null) {
                String parameterGroupValue = String.valueOf(sdkMap.get("parameterGroup"));
                System.out.println(">> Found parameterGroup: " + parameterGroupValue);
                field.annotate(ParameterGroup.class).param("name", parameterGroupValue);
            }
		}
	}
}
