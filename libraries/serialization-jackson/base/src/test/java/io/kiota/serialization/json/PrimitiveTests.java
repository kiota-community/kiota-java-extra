package io.kiota.serialization.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.microsoft.kiota.serialization.SerializationWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class PrimitiveTests {

    @Test
    public void produceCorrectStringOnlyElement() throws IOException {
        // Arrange
        SerializationWriter writer =
                new JsonSerializationWriterFactory().getSerializationWriter("application/json");

        // Act
        writer.writeStringValue(null, "foo");
        String result =
                new String(writer.getSerializedContent().readAllBytes(), StandardCharsets.UTF_8);

        // Assert
        assertEquals("\"foo\"", result);
    }
}
