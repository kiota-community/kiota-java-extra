package io.kiota.serialization.json;

import static io.kiota.serialization.json.JsonMapper.mapper;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.microsoft.kiota.serialization.SerializationWriter;
import java.io.IOException;
import java.math.BigDecimal;
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

    @Test
    public void deserializePrimitiveTypes() throws IOException {
        // Arrange
        JsonParseNode node = new JsonParseNode(mapper.readTree("123"));

        // Act
        var intValue = node.getIntegerValue();
        var longValue = node.getLongValue();
        var shortValue = node.getShortValue();
        var doubleValue = node.getDoubleValue();
        var floatValue = node.getFloatValue();
        var bigDecValue = node.getBigDecimalValue();
        var byteValue = node.getByteValue();

        // Assert
        assertEquals(123, intValue);
        assertEquals(123L, longValue);
        assertEquals((short)123, shortValue);
        assertEquals(123.0, doubleValue);
        assertEquals(123.0f, floatValue);
        assertEquals(new BigDecimal(123), bigDecValue);
        assertEquals((byte)123, byteValue);
    }
}
