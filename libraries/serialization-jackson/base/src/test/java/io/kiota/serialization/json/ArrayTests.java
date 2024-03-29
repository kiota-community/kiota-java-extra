package io.kiota.serialization.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.microsoft.kiota.serialization.SerializationWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ArrayTests {

    @Test
    public void produceCorrectArrayOfElements() throws IOException {
        // Arrange
        SerializationWriter writer =
                new JsonSerializationWriterFactory().getSerializationWriter("application/json");

        // Act
        writer.writeCollectionOfPrimitiveValues(null, List.of("one", "two", "three"));
        String result =
                new String(writer.getSerializedContent().readAllBytes(), StandardCharsets.UTF_8);

        // Assert
        assertEquals("[\"one\",\"two\",\"three\"]", result);
    }
}
