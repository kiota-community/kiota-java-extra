package io.kiota.serialization.json.quarkus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

public class JsonParseNodeFactory extends io.kiota.serialization.json.JsonParseNodeFactory {

    private ObjectMapper mapper;

    public JsonParseNodeFactory(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected ObjectReader getObjectReader() {
        return mapper.reader();
    }
}
