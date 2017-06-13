package org.mule.custom.devkit.utils;

import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.jsonschema2pojo.AbstractAnnotator;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Ignore;
import org.mule.api.annotations.display.Placement;
import org.mule.api.annotations.param.Default;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JFieldVar;

/**
 * Custom Devkit Annotator
 *
 */
public class CustomDevkitAnnotator extends AbstractAnnotator {
	@Override
	  public void propertyField( JFieldVar field, JDefinedClass clazz, String propertyName, JsonNode propertyNode ) {
	    super.propertyField(field, clazz, propertyName, propertyNode);

	    ObjectMapper mapper = new ObjectMapper();
	    Map<String, Object> jsonMap = mapper.convertValue(propertyNode, Map.class);
	    Map<String, Object> devkitMap = (Map<String, Object>) jsonMap.get("devkit");
	    System.out.println("********************** devkitMap: " + devkitMap);

	    if (devkitMap != null) {
	    	if (devkitMap.get("placement") != null) {
		    	String placement = (String) devkitMap.get("placement");
		    	field.annotate(Placement.class).param("group", placement);
	    	}
	    	
	    	String defaultValue = (String) devkitMap.get("default");
	    	field.annotate(Default.class).param("value", defaultValue);
	    	
	    	if (Boolean.TRUE.equals(devkitMap.get("isConfig"))) 
	    		field.annotate(Configurable.class);
	    	
	    	if (Boolean.TRUE.equals(devkitMap.get("isHidden")))
		    	field.annotate(Ignore.class);
	    	
	    	
	    }
	}
}
