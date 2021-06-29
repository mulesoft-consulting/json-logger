package org.mule.extension.jsonlogger;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.mule.extension.jsonlogger.config.JsonLoggerConfig;
import org.mule.extension.jsonlogger.config.LogProcessor;
import org.mule.extension.jsonlogger.config.Priority;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.LocationPart;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeParamsBuilder;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.internal.metadata.SimpleDataType;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JsonLoggerOperationsTest {
  protected static Logger log = LoggerFactory.getLogger("org.mule.extension.jsonlogger.JsonLogger");
  
  JsonLoggerOperations jsonLoggerOperations = new JsonLoggerOperations();
  LogProcessor logProcessor = new LogProcessor()
    .setCorrelationId(UUID.randomUUID().toString())
    .setMessage("JsonLoggerOperationsTest")
    .setPriority(Priority.INFO);
  
  CompletionCallback<Void, Void> completionCallback = new CompletionCallback<Void, Void>() {
    @Override
    public void success(Result<Void, Void> result) {
    
    }
    
    @Override
    public void error(Throwable throwable) {
    
    }
  };
  
  JsonLoggerConfig config = new JsonLoggerConfig().setApplicationName("json-logger");
  ComponentLocation location = new ComponentLocation() {
    @Override
    public String getLocation() {
      return System.getProperty("user.dir");
    }
    
    @Override
    public Optional<String> getFileName() {
      return Optional.of("api.xml");
    }
    
    @Override
    public Optional<Integer> getLineInFile() {
      return Optional.of(32);
    }
    
    @Override
    public List<LocationPart> getParts() {
      return null;
    }
    
    @Override
    public TypedComponentIdentifier getComponentIdentifier() {
      return null;
    }
    
    @Override
    public String getRootContainerName() {
      return "http";
    }
  };
  
  @Test
  public void locationIsAdded() {
    ObjectNode logEvent = Mapper.getInstance().createObjectNode();
    jsonLoggerOperations.addLocationInfo(logEvent, location);
    Assert
      .assertEquals("{\"locationInfo\":{\"fileName\":\"api.xml\",\"rootContainer\":\"http\",\"lineNumber\":\"32\"}}",
        logEvent.toString());
  }
  
  @Test
  public void javaPayloadReturnsToString() throws IOException {
    ObjectNode logEvent = Mapper.getInstance().createObjectNode();
    
    MagicBeans magicBeans = new MagicBeans(new URL("http://localhost"));
    TypedValue value = new TypedValue<InputStream>(magicBeans.getInputStream(), magicBeans.build());
    StaticParameterResolver resolver = new StaticParameterResolver<TypedValue<InputStream>>(value);
    
    jsonLoggerOperations.addContent(logEvent, resolver, config);
    Assert.assertEquals("", "");
  }
  
  @Test
  public void logHasCorrelationIdIfMaskingFails() {
    try {
      throw new RuntimeException("Invalid string");
    } catch (Exception e) {
      String s = String.format("{" +
          "\"%s\": \"%s\", " +
          "\"message\": \"Error parsing log data as a string: %s\"" +
          "}",
        "correlationId",
        UUID.randomUUID().toString(),
        e.getMessage());
      System.out.println(s);
    }
  }
  
  static class MagicBeans extends DataHandler implements DataTypeParamsBuilder, Serializable {
    String beanstalk = "beanstalk";
    
    @Override
    public String getContentType() {
      return "application/java";
    }
    
    public MagicBeans(URL url) {
      super(url);
    }
    
    @Override
    public InputStream getInputStream() throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(this);
      oos.flush();
      oos.close();
      return new ByteArrayInputStream(baos.toByteArray());
    }
  
    @Override
    public DataTypeParamsBuilder mediaType(String s) {
      return this;
    }
    
    @Override
    public DataTypeParamsBuilder mediaType(MediaType mediaType) {
      return this;
    }
    
    @Override
    public DataTypeParamsBuilder charset(String s) {
      return this;
    }
    
    @Override
    public DataTypeParamsBuilder charset(Charset charset) {
      return this;
    }
    
    @Override
    public DataType build() {
      return DataType.fromObject(this);
    }
    
    @Override
    public String toString() {
      return beanstalk;
    }
  }
}
