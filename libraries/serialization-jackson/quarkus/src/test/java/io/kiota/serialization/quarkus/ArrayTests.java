package io.kiota.serialization.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.kiota.serialization.SerializationWriter;
import io.kiota.serialization.json.quarkus.JsonSerializationWriterFactory;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ArrayTests {

    ObjectMapper mapper = new ObjectMapper();

    @Test
    public void produceCorrectArrayOfElements() throws IOException {
        // Arrange
        JsonSerializationWriterFactory jsonSerializationWriterFactory =
                new JsonSerializationWriterFactory(mapper);
        SerializationWriter writer =
                jsonSerializationWriterFactory.getSerializationWriter("application/json");

        // Act
        writer.writeCollectionOfPrimitiveValues(null, List.of("one", "two", "three"));
        String result =
                new String(writer.getSerializedContent().readAllBytes(), StandardCharsets.UTF_8);

        // Assert
        assertEquals("[\"one\",\"two\",\"three\"]", result);
    }
}
