package io.kiota.serialization.json.quarkus;

import com.fasterxml.jackson.databind.ObjectReader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class JsonParseNodeFactory extends io.kiota.serialization.json.JsonParseNodeFactory {
    @Inject private JsonMapper mapper;

    JsonParseNodeFactory(JsonMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected ObjectReader getObjectReader() {
        return mapper.getObjectReader();
    }
}
