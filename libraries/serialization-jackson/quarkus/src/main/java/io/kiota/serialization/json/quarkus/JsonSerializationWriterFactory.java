package io.kiota.serialization.json.quarkus;

import com.fasterxml.jackson.databind.ObjectWriter;
import jakarta.inject.Inject;

/** Creates new Json serialization writers. */
public class JsonSerializationWriterFactory
        extends io.kiota.serialization.json.JsonSerializationWriterFactory {
    @Inject JsonMapper mapper;

    public JsonSerializationWriterFactory(JsonMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected ObjectWriter getObjectWriter() {
        return mapper.getObjectWriter();
    }
}
