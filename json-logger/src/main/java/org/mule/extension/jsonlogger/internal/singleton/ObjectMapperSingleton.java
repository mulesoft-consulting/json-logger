package org.mule.extension.jsonlogger.internal.singleton;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.util.List;

public class ObjectMapperSingleton {
  
  private final ObjectMapper om = new ObjectMapper()
    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
  
  private final ObjectMapper maskingMapper = new ObjectMapper()
    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
  
  
  public ObjectMapper getObjectMapper(List<String> maskedFields) {
    SimpleModule module = new SimpleModule();
    module.addDeserializer(JsonNode.class, new MaskingDeserializer(maskedFields));
    this.maskingMapper.registerModule(module);
    return this.maskingMapper;
  }
  
  public ObjectMapper getObjectMapper() {
    return this.om;
  }
}
