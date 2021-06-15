
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.beanutils.PropertyUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mule.extension.jsonlogger.api.pojos.LoggerProcessor;
import org.mule.extension.jsonlogger.internal.singleton.ObjectMapperSingleton;
import org.mule.runtime.api.metadata.TypedValue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JsonMaskerTest {
  ClassLoader classloader = Thread.currentThread().getContextClassLoader();
  InputStream is = classloader.getResourceAsStream("payload.json");
  List<String> maskedFields = Collections.singletonList("attributes");
  private final ObjectMapper om = new ObjectMapperSingleton().getObjectMapper(maskedFields);
  
  @Test
  public void caseInsensitve() throws IOException {
    JsonNode payload = om.readTree(is);
    Map<String, Object> masked = om.convertValue(payload, new TypeReference<Map<String, Object>>() {
    });
    Assert.assertEquals("{name=****, phone=************, gender=****}", masked.get("attributes").toString());
  }
  
  @Test
  public void typedValue() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
    LoggerProcessor loggerProcessor = new LoggerProcessor();
    Map<String, Object> describe = PropertyUtils.describe(loggerProcessor);
    describe.forEach((k,v) -> {
      System.out.println(k);
    });
  }
  
  
}