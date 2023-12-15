package io.kiota.serialization.json.quarkus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Singleton;

@Singleton
@Unremovable
/** Creates new Json serialization writers. */
public class JsonSerializationWriterFactory
        extends io.kiota.serialization.json.JsonSerializationWriterFactory {
    private ObjectMapper mapper;

    public JsonSerializationWriterFactory(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected ObjectWriter getObjectWriter() {
        return mapper.writer();
    }
}
