package io.kiota.serialization.json;

import static io.kiota.serialization.json.JsonMapper.mapper;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.kiota.serialization.Parsable;
import com.microsoft.kiota.serialization.ParseNode;
import com.microsoft.kiota.serialization.SerializationWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class CustomJsonParseNodeTests {

    private static class TestObject implements Parsable {
        private Integer integer;
        private Integer integer2;
        private OffsetDateTime dateTime;

        public Integer getInteger() {
            return integer;
        }

        public void setInteger(Integer integer) {
            this.integer = integer;
        }

        public Integer getInteger2() {
            return integer2;
        }

        public void setInteger2(Integer integer2) {
            this.integer2 = integer2;
        }

        public OffsetDateTime getDateTime() {
            return dateTime;
        }

        public void setDateTime(OffsetDateTime dateTime) {
            this.dateTime = dateTime;
        }

        @jakarta.annotation.Nonnull
        public static TestObject createFromDiscriminatorValue(
                @jakarta.annotation.Nonnull final ParseNode parseNode) {
            Objects.requireNonNull(parseNode);
            return new TestObject();
        }

        @Override
        public Map<String, Consumer<ParseNode>> getFieldDeserializers() {
            final HashMap<String, java.util.function.Consumer<ParseNode>> deserializerMap =
                    new HashMap<String, java.util.function.Consumer<ParseNode>>(9);
            deserializerMap.put(
                    "integer",
                    n -> {
                        this.setInteger(n.getIntegerValue());
                    });
            deserializerMap.put(
                    "integer2",
                    n -> {
                        this.setInteger2(n.getIntegerValue());
                    });
            deserializerMap.put(
                    "dateTime",
                    n -> {
                        this.setDateTime(n.getOffsetDateTimeValue());
                    });
            return deserializerMap;
        }

        @Override
        public void serialize(SerializationWriter writer) {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    void testIntegerConversion() throws IOException {
        class DivideEvenIntegersJsonParseNode extends JsonParseNode {
            DivideEvenIntegersJsonParseNode(JsonParseNodeFactory factory, JsonNode currentNode) {
                super(factory, currentNode);
            }

            @Override
            public Integer getIntegerValue() {
                Integer value = super.getIntegerValue();

                if (value != null && value % 2 == 0) {
                    value = value / 2;
                }

                return value;
            }
        }

        JsonParseNodeFactory factory =
                new JsonParseNodeFactory() {
                    @Override
                    protected JsonParseNode createJsonParseNode(JsonNode currentNode) {
                        return new DivideEvenIntegersJsonParseNode(this, currentNode);
                    }
                };

        JsonParseNode node =
                factory.createJsonParseNode(mapper.readTree("{\"integer\":7,\"integer2\":42}"));

        var objValue = node.getObjectValue(TestObject::createFromDiscriminatorValue);
        assertEquals(7, objValue.getInteger());
        assertEquals(21, objValue.getInteger2());
    }

    @Test
    void testCustomDateParser() throws IOException {
        class CustomDateFormatJsonParseNode extends JsonParseNode {
            CustomDateFormatJsonParseNode(JsonParseNodeFactory factory, JsonNode currentNode) {
                super(factory, currentNode);
            }

            @Override
            public OffsetDateTime getOffsetDateTimeValue() {
                if (currentNode.isTextual()) {
                    /**
                     * Receive a date in DMY format, but we need a timestamp
                     * at UTC start of day.
                     */
                    return LocalDate.parse(
                                    currentNode.textValue(),
                                    DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                            .atStartOfDay()
                            .atOffset(ZoneOffset.UTC);
                }
                return null;
            }
        }

        JsonParseNodeFactory factory =
                new JsonParseNodeFactory() {
                    @Override
                    protected JsonParseNode createJsonParseNode(JsonNode currentNode) {
                        return new CustomDateFormatJsonParseNode(this, currentNode);
                    }
                };

        JsonParseNode node =
                factory.createJsonParseNode(mapper.readTree("{\"dateTime\":\"28-02-2025\"}"));

        var objValue = node.getObjectValue(TestObject::createFromDiscriminatorValue);
        assertEquals(OffsetDateTime.parse("2025-02-28T00:00:00Z"), objValue.getDateTime());
    }
}
