package org.mule.extension.jsonlogger.internal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.mule.extension.jsonlogger.internal.mapper.ObjectMapperSingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JsonMaskerTest {
  ClassLoader classloader = Thread.currentThread().getContextClassLoader();
  InputStream is = classloader.getResourceAsStream("payload.json");
  List<String> maskedFields = Collections.singletonList("attributes");
  private final ObjectMapper om = new ObjectMapperSingleton().getObjectMapper(maskedFields);
  protected Logger log = LoggerFactory.getLogger("org.mule.extension.jsonlogger.JsonLogger");
  
  @Test
  public void caseInsensitve() throws IOException {
    JsonNode payload = om.readTree(is);
    Map<String, Object> masked = om.convertValue(payload, new TypeReference<Map<String, Object>>() {
    });
    Assert.assertEquals("{name=****, phone=************, gender=****}", masked.get("attributes").toString());
  }
  
  @Test
  public void finalException() {
    try {
      throw new RuntimeException("Invalid string");
    } catch (Exception e) {
      String s = String.format("{" +
          "\"%s\": \"%s\", " +
          "\"message\": \"Error parsing log data as a string: %s\"" +
          "}",
        Constants.CORRELATION_ID,
        UUID.randomUUID().toString(),
        e.getMessage());
      System.out.println(s);
    }
  }
  
  
}