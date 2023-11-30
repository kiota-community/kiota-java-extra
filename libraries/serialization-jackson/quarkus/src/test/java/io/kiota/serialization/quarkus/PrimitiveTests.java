package io.kiota.serialization.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.microsoft.kiota.serialization.SerializationWriter;
import io.kiota.serialization.json.quarkus.JsonSerializationWriterFactory;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class PrimitiveTests {

    @Inject JsonSerializationWriterFactory serializationWriterFactory;

    @Test
    public void produceCorrectStringOnlyElement() throws IOException {
        // Arrange
        SerializationWriter writer =
                serializationWriterFactory.getSerializationWriter("application/json");

        // Act
        writer.writeStringValue(null, "foo");
        String result =
                new String(writer.getSerializedContent().readAllBytes(), StandardCharsets.UTF_8);

        // Assert
        assertEquals("\"foo\"", result);
    }
}
