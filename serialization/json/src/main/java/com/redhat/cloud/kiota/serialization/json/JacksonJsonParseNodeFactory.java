package com.redhat.cloud.kiota.serialization.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.kiota.serialization.ParseNode;
import com.microsoft.kiota.serialization.ParseNodeFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class JacksonJsonParseNodeFactory implements ParseNodeFactory {

    private final String validContentType = "application/json";

    @Nonnull
    @Override
    public String getValidContentType() {
        return validContentType;
    }

    @Nonnull
    @Override
    public ParseNode getParseNode(@Nonnull String contentType, @Nonnull InputStream rawResponse) {
        Objects.requireNonNull(contentType, "parameter contentType cannot be null");
        Objects.requireNonNull(rawResponse, "parameter rawResponse cannot be null");
        if(contentType.isEmpty()) {
            throw new NullPointerException("contentType cannot be empty");
        } else if (!contentType.equals(validContentType)) {
            throw new IllegalArgumentException("expected a " + validContentType + " content type");
        }
        try(final InputStreamReader reader = new InputStreamReader(rawResponse, StandardCharsets.UTF_8)) {
            JsonFactory jsonFactory = new JsonFactory();
            ObjectMapper objectMapper = new ObjectMapper();

            return new JacksonJsonParseNode(objectMapper.readTree(reader));
        } catch (IOException ex) {
            throw new RuntimeException("could not close the reader", ex);
        }
    }
}
