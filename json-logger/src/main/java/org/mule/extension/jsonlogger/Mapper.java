package org.mule.extension.jsonlogger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class Mapper {
  volatile static boolean isInitalized;
  
  private final ObjectMapper om = new ObjectMapper()
    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
  
  private final ObjectMapper maskingMapper = new ObjectMapper()
    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
  
  private Mapper() {
  }
  
  private static class MapHolder {
    static final ObjectMapper INSTANCE = new Mapper().om;
    static final ObjectMapper MASKING_INSTANCE = new Mapper().maskingMapper;
  }
  
  public static ObjectMapper getInstance(String contentFieldsDataMasking) {
    if (contentFieldsDataMasking == null || contentFieldsDataMasking.isEmpty()) {
      return MapHolder.INSTANCE;
    }
    buildMaskedFields(contentFieldsDataMasking);
    return MapHolder.MASKING_INSTANCE;
  }
  
  public static ObjectMapper getInstance() {
    return MapHolder.INSTANCE;
  }
  
  private static void buildMaskedFields(String contentFieldsDataMasking) {
    while (!isInitalized) {
      Set<String> maskedFields = new HashSet<>();
      AtomicBoolean lock = new AtomicBoolean(false);
      if (lock.compareAndSet(false, true)) {
        try {
          if (contentFieldsDataMasking != null && !contentFieldsDataMasking.isEmpty()) {
            String[] split = contentFieldsDataMasking
              .trim()
              .split(",");
            
            for (String s : split) {
              maskedFields.add(s.trim());
            }
            SimpleModule module = new SimpleModule()
              .addDeserializer(JsonNode.class, new MaskingDeserializer(maskedFields));
            MapHolder.MASKING_INSTANCE.registerModule(module);
          }
        } finally {
          isInitalized = true;
          lock.set(false);
        }
      }
    }
  }
}
