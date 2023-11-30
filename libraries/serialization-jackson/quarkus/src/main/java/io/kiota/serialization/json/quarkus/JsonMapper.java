package io.kiota.serialization.json.quarkus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

// @ApplicationScoped
@Dependent
public class JsonMapper {
    private ObjectMapper mapper;

    @Inject
    JsonMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public ObjectReader getObjectReader() {
        return mapper.reader();
    }

    public ObjectWriter getObjectWriter() {
        return mapper.writer();
    }
}
