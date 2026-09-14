package io.kiota.serialization.json;

import static io.kiota.serialization.json.JsonMapper.mapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.microsoft.kiota.serialization.SerializationWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
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
        JsonParseNodeFactory factory = new JsonParseNodeFactory();
        JsonParseNode node = factory.createJsonParseNode(mapper.readTree("123"));

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
        assertEquals((short) 123, shortValue);
        assertEquals(123.0, doubleValue);
        assertEquals(123.0f, floatValue);
        assertEquals(new BigDecimal(123), bigDecValue);
        assertEquals((byte) 123, byteValue);
    }

    @Test
    public void deserializeOffsetDateTimeWithOffset() throws IOException {
        JsonParseNodeFactory factory = new JsonParseNodeFactory();
        JsonParseNode node = factory.createJsonParseNode(mapper.readTree("\"2024-02-08T12:07:31Z\""));

        OffsetDateTime value = node.getOffsetDateTimeValue();
        assertEquals(OffsetDateTime.of(2024, 2, 8, 12, 7, 31, 0, ZoneOffset.UTC), value);
    }

    @Test
    public void deserializeOffsetDateTimeWithoutOffsetFallsBackToUtc() throws IOException {
        JsonParseNodeFactory factory = new JsonParseNodeFactory();
        JsonParseNode node = factory.createJsonParseNode(mapper.readTree("\"2024-02-08T12:07:31\""));

        OffsetDateTime value = node.getOffsetDateTimeValue();
        assertEquals(OffsetDateTime.of(2024, 2, 8, 12, 7, 31, 0, ZoneOffset.UTC), value);
    }

    @Test
    public void deserializeOffsetDateTimeInvalidThrowsException() throws IOException {
        JsonParseNodeFactory factory = new JsonParseNodeFactory();
        JsonParseNode node = factory.createJsonParseNode(mapper.readTree("\"not-a-date\""));

        assertThrows(DateTimeParseException.class, () -> node.getOffsetDateTimeValue());
    }

    @Test
    public void deserializeOffsetDateTimeNullNodeReturnsNull() throws IOException {
        JsonParseNodeFactory factory = new JsonParseNodeFactory();
        JsonParseNode node = factory.createJsonParseNode(mapper.readTree("null"));

        assertNull(node.getOffsetDateTimeValue());
    }
}
