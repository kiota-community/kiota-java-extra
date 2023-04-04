package com.redhat.cloud.kiota.serialization.json;

import com.microsoft.kiota.serialization.SerializationWriter;
import com.microsoft.kiota.serialization.SerializationWriterFactory;

import javax.annotation.Nonnull;
import java.util.Objects;

public class JacksonJsonSerializationWriterFactory implements SerializationWriterFactory {

    private final String validContentType = "application/json";

    @Override
    @Nonnull
    public String getValidContentType() {
        return validContentType;
    }

    @Override
    @Nonnull
    public SerializationWriter getSerializationWriter(@Nonnull String contentType) {
        Objects.requireNonNull(contentType, "parameter contentType cannot be null");
        if (contentType.isEmpty()) {
            throw new NullPointerException("contentType cannot be empty");
        } else if (!contentType.equals(validContentType)) {
            throw new IllegalArgumentException("expected a " + validContentType + " content type");
        }
        return new JacksonJsonSerializationWriter();
    }
}
