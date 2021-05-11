
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.mule.extension.jsonlogger.internal.singleton.ObjectMapperSingleton;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
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
    Map<String, Object> masked = om.convertValue(payload, new TypeReference<Map<String, Object>>(){});
    Assert.assertEquals("{name=****, phone=************, gender=****}", masked.get("attributes").toString());
  }
}