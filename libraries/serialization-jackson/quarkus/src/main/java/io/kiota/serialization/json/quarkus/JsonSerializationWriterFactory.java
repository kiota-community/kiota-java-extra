package io.kiota.serialization.json.quarkus;

import com.fasterxml.jackson.databind.ObjectWriter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Creates new Json serialization writers. */
@ApplicationScoped
public class JsonSerializationWriterFactory
        extends io.kiota.serialization.json.JsonSerializationWriterFactory {
    @Inject JsonMapper mapper;

    JsonSerializationWriterFactory(JsonMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected ObjectWriter getObjectWriter() {
        return mapper.getObjectWriter();
    }
}
