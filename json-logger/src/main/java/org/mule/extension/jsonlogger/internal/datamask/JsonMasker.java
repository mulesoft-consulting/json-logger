package org.mule.extension.jsonlogger.internal.datamask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.*;
import java.util.regex.Pattern;

public class JsonMasker {
  
  private static final Pattern digits = Pattern.compile("\\d");
  private static final Pattern capitalLetters = Pattern.compile("[A-Z]");
  private static final Pattern nonSpecialCharacters = Pattern.compile("[^X\\s!-/:-@\\[-`{-~]");
  
  private final Set<String> blacklistedKeys;
  private final boolean enabled;
  
  public JsonMasker(Collection<String> blacklist, boolean enabled) {
    this.enabled = enabled;
    
    blacklistedKeys = new HashSet<>();
    
    blacklist.forEach(item -> {
      blacklistedKeys.add(item.toUpperCase());
    });
  }
  
  public JsonNode mask(JsonNode target) {
    if (!enabled)
      return target;
    if (target == null)
      return null;
    
    return traverseAndMask(target.deepCopy(), "$", false);
  }
  
  private JsonNode traverseAndMask(JsonNode target, String path, Boolean isBlackListed) {
    if (target.isTextual() && isBlackListed) {
      return new TextNode(maskString(target.asText()));
    }
    if (target.isNumber() && isBlackListed) {
      return new TextNode(maskNumber(target.asText()));
    }
    
    if (target.isObject()) {
      Iterator<Map.Entry<String, JsonNode>> fields = target.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        String childPath = appendPath(path, field.getKey());
        if (blacklistedKeys.contains(field.getKey().toUpperCase()) || isBlackListed) {
          ((ObjectNode) target).replace(field.getKey(), traverseAndMask(field.getValue(), childPath, true));
        } else {
          ((ObjectNode) target).replace(field.getKey(), traverseAndMask(field.getValue(), childPath, false));
        }
      }
    }
    if (target.isArray()) {
      for (int i = 0; i < target.size(); i++) {
        String childPath = appendPath(path, i);
        if (isBlackListed) {
          ((ArrayNode) target).set(i, traverseAndMask(target.get(i), childPath, true));
        } else {
          ((ArrayNode) target).set(i, traverseAndMask(target.get(i), childPath, false));
        }
      }
    }
    return target;
  }
  
  private static String appendPath(String path, String key) {
    return path + "['" + key + "']";
  }
  
  private static String appendPath(String path, int ind) {
    return path + "[" + ind + "]";
  }
  
  private static String maskString(String value) {
    String tmpMasked = digits.matcher(value).replaceAll("*");
    tmpMasked = capitalLetters.matcher(tmpMasked).replaceAll("X");
    return nonSpecialCharacters.matcher(tmpMasked).replaceAll("x");
  }
  
  private static String maskNumber(String value) {
    return value.replaceAll("[0-9]", "*");
  }
  
}