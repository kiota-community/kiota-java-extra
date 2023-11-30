package io.kiota.serialization.json.quarkus;

import com.fasterxml.jackson.databind.ObjectReader;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

// @ApplicationScoped
@Dependent
public class JsonParseNodeFactory extends io.kiota.serialization.json.JsonParseNodeFactory {
    @Inject private JsonMapper mapper;

    @Inject
    JsonParseNodeFactory(JsonMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected ObjectReader getObjectReader() {
        return mapper.getObjectReader();
    }
}
