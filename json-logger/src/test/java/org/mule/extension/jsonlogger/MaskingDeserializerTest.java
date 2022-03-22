package org.mule.extension.jsonlogger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("rawTypes, unchecked")
public class MaskingDeserializerTest {
  static String fieldsToBeMasked = "name, phone, documents";
  static Map<String, Object> masked;
  
  @BeforeClass
  public static void setup() throws IOException {
    InputStream is = Thread
      .currentThread()
      .getContextClassLoader()
      .getResourceAsStream("pii.json");
    JsonNode payload = Mapper.getInstance(fieldsToBeMasked).readTree(is);
    masked = Mapper
      .getInstance()
      .convertValue(payload, new TypeReference<Map<String, Object>>() {
      });
  }
  
  @Test
  public void fieldsAreBeingMasked() {
    LinkedHashMap<String, String> passenger = (LinkedHashMap) masked.get("passenger");
    Assert.assertEquals("**** ***", passenger.get("name"));
  }
  
  @Test
  public void formatIsBeingMaintained() {
    LinkedHashMap<String, String> passenger = (LinkedHashMap) masked.get("passenger");
    Assert.assertEquals("***-***-****", passenger.get("phone"));
  }
  
  @Test
  public void onlyRelevantFieldsGetMasked() {
    LinkedHashMap destination = (LinkedHashMap) masked.get("destination");
    Assert.assertEquals("Phoenix", destination.get("city"));
    LinkedHashMap passenger = (LinkedHashMap) masked.get("passenger");
    Assert.assertEquals("{city=Dallas}", passenger.get("address").toString());
    
  }
  
  @Test
  public void nestedMasking() {
    Assert.assertEquals("{passport=********}", masked.get("documents").toString());
  }
}