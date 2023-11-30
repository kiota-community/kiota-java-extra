package io.kiota.serialization.json;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonMapper {

    private JsonMapper() {}

    // We use a public mapper so that ppl can hack it around if needed.
    public static final ObjectMapper mapper = new ObjectMapper();
}
