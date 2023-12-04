package io.kiota.serialization.json.quarkus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@DefaultBean
public class JsonMapper {
    @Inject ObjectMapper mapper;

    @Inject
    JsonMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Produces
    @DefaultBean
    public JsonParseNodeFactory jsonParseNodeFactory() {
        return new JsonParseNodeFactory(this);
    }

    @Produces
    @DefaultBean
    public JsonSerializationWriterFactory jsonSerializationWriterFactory() {
        return new JsonSerializationWriterFactory(this);
    }

    public ObjectReader getObjectReader() {
        return mapper.reader();
    }

    public ObjectWriter getObjectWriter() {
        return mapper.writer();
    }
}
