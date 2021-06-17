package org.mule.extension.jsonlogger.internal.mapper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.JsonNodeDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.List;

class MaskingDeserializer extends JsonNodeDeserializer {
  List<String> maskedFields;
  
  public MaskingDeserializer(List<String> maskedFields) {
    this.maskedFields = maskedFields;
  }
  
  @Override
  public JsonNode deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    switch (p.currentTokenId()) {
      case 1:
        return deserializeObject(p, ctxt, ctxt.getNodeFactory(), false);
      case 3:
        return deserializeArray(p, ctxt, ctxt.getNodeFactory(), false);
      default:
        return deserializeAny(p, ctxt, ctxt.getNodeFactory(), false);
    }
  }
  
  protected final ObjectNode deserializeObject(JsonParser p, DeserializationContext ctxt, JsonNodeFactory nodeFactory, boolean shouldMask) throws IOException {
    ObjectNode node = nodeFactory.objectNode();
    
    for (String key = p.nextFieldName(); key != null; key = p.nextFieldName()) {
      if (maskedFields.contains(key)) {
        shouldMask = true;
      }
      JsonToken t = p.nextToken();
      if (t == null) {
        t = JsonToken.NOT_AVAILABLE;
      }
      
      JsonNode value;
      switch (t.id()) {
        case 1:
          value = deserializeObject(p, ctxt, nodeFactory, shouldMask);
          shouldMask = false;
          break;
        case 2:
        case 4:
        case 5:
        case 8:
        default:
          value = deserializeAny(p, ctxt, nodeFactory, shouldMask);
          shouldMask = false;
          break;
        case 3:
          value = deserializeArray(p, ctxt, nodeFactory, shouldMask);
          shouldMask = false;
          break;
        case 6:
          if (shouldMask) {
            value = nodeFactory.textNode(replace(p.getText()));
          } else {
            value = nodeFactory.textNode(p.getText());
          }
          break;
        case 7:
          value = this._fromInt(p, ctxt, nodeFactory);
          break;
        case 9:
          value = nodeFactory.booleanNode(true);
          break;
        case 10:
          value = nodeFactory.booleanNode(false);
          break;
        case 11:
          value = nodeFactory.nullNode();
          break;
        case 12:
          value = this._fromEmbedded(p, ctxt, nodeFactory);
      }
      
      JsonNode old = node.replace(key, value);
      if (old != null) {
        this._handleDuplicateField(p, ctxt, nodeFactory, key, node, old, value);
      }
    }
    
    return node;
  }
  
  protected final ArrayNode deserializeArray(JsonParser p, DeserializationContext ctxt, JsonNodeFactory nodeFactory, boolean shouldMask) throws IOException {
    ArrayNode node = nodeFactory.arrayNode();
    
    while (true) {
      JsonToken t = p.nextToken();
      switch (t.id()) {
        case 1:
          node.add(deserializeObject(p, ctxt, nodeFactory, shouldMask));
          break;
        case 2:
        case 5:
        case 8:
        default:
          node.add(deserializeAny(p, ctxt, nodeFactory, shouldMask));
          break;
        case 3:
          node.add(deserializeArray(p, ctxt, nodeFactory, shouldMask));
          break;
        case 4:
          return node;
        case 6:
          if (shouldMask) {
            node.add(nodeFactory.textNode(replace(p.getText())));
          } else {
            node.add(nodeFactory.textNode(p.getText()));
          }
          break;
        case 7:
          node.add(this._fromInt(p, ctxt, nodeFactory));
          break;
        case 9:
          node.add(nodeFactory.booleanNode(true));
          break;
        case 10:
          node.add(nodeFactory.booleanNode(false));
          break;
        case 11:
          node.add(nodeFactory.nullNode());
          break;
        case 12:
          node.add(this._fromEmbedded(p, ctxt, nodeFactory));
      }
    }
  }
  
  protected final JsonNode deserializeAny(JsonParser p, DeserializationContext ctxt, JsonNodeFactory nodeFactory, boolean shouldMask) throws IOException {
    switch (p.currentTokenId()) {
      case 2:
        return nodeFactory.objectNode();
      case 3:
      case 4:
      default:
        return (JsonNode) ctxt.handleUnexpectedToken(this.handledType(), p);
      case 5:
        return deserializeObjectAtName(p, ctxt, nodeFactory);
      case 6:
        if (shouldMask) {
          return nodeFactory.textNode(replace(p.getText()));
        }
        return nodeFactory.textNode(p.getText());
      case 7:
        return this._fromInt(p, ctxt, nodeFactory);
      case 8:
        return this._fromFloat(p, ctxt, nodeFactory);
      case 9:
        return nodeFactory.booleanNode(true);
      case 10:
        return nodeFactory.booleanNode(false);
      case 11:
        return nodeFactory.nullNode();
      case 12:
        return this._fromEmbedded(p, ctxt, nodeFactory);
    }
  }
  
  private String replace(String input) {
    return StringUtils.repeat("*", input.length());
  }
  
}
