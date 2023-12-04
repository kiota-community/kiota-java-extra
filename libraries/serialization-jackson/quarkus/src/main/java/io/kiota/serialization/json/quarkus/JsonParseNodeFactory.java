package io.kiota.serialization.json.quarkus;

import com.fasterxml.jackson.databind.ObjectReader;
import jakarta.inject.Inject;

public class JsonParseNodeFactory extends io.kiota.serialization.json.JsonParseNodeFactory {

    @Inject JsonMapper mapper;

    public JsonParseNodeFactory(JsonMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected ObjectReader getObjectReader() {
        return mapper.getObjectReader();
    }
}
