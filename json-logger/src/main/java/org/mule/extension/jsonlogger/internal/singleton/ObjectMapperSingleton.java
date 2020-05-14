package org.mule.extension.jsonlogger.internal.singleton;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class ObjectMapperSingleton {

    // JSON Object Mapper
    private final ObjectMapper om = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
//            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
//            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    public ObjectMapper getObjectMapper() {
        return this.om;
    }
}
