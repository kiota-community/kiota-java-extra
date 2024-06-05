package io.kiota.serialization.json;

import static io.kiota.serialization.json.JsonMapper.mapper;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.microsoft.kiota.serialization.AdditionalDataHolder;
import com.microsoft.kiota.serialization.Parsable;
import com.microsoft.kiota.serialization.ParseNode;
import com.microsoft.kiota.serialization.SerializationWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

public class ObjectTypeTests {

    private static class TestObject implements Parsable, AdditionalDataHolder {
        private String string;
        private Integer integer;
        private Long lo;
        private Boolean bool;
        private Float flo;
        private Double doub;
        // This test fails only if getAdditionalData != null
        private Map<String, Object> additionalData = new HashMap<String, Object>();

        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }

        public Integer getInteger() {
            return integer;
        }

        public void setInteger(Integer integer) {
            this.integer = integer;
        }

        public Long getLo() {
            return lo;
        }

        public void setLo(Long lo) {
            this.lo = lo;
        }

        public Boolean getBool() {
            return bool;
        }

        public void setBool(Boolean bool) {
            this.bool = bool;
        }

        public Float getFlo() {
            return flo;
        }

        public void setFlo(Float flo) {
            this.flo = flo;
        }

        public Double getDoub() {
            return doub;
        }

        public void setDoub(Double doub) {
            this.doub = doub;
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
                    "string",
                    (n) -> {
                        this.setString(n.getStringValue());
                    });
            deserializerMap.put(
                    "integer",
                    (n) -> {
                        this.setInteger(n.getIntegerValue());
                    });
            deserializerMap.put(
                    "lo",
                    (n) -> {
                        this.setLo(n.getLongValue());
                    });
            deserializerMap.put(
                    "bool",
                    (n) -> {
                        this.setBool(n.getBooleanValue());
                    });
            deserializerMap.put(
                    "flo",
                    (n) -> {
                        this.setFlo(n.getFloatValue());
                    });
            deserializerMap.put(
                    "doub",
                    (n) -> {
                        this.setDoub(n.getDoubleValue());
                    });
            return deserializerMap;
        }

        @Override
        public void serialize(SerializationWriter writer) {
            // TODO Auto-generated method stub

        }

        @Override
        public Map<String, Object> getAdditionalData() {
            return additionalData;
        }
    }

    @Test
    public void deserializeObjectType() throws IOException {
        // Arrange
        JsonParseNode node =
                new JsonParseNode(
                        mapper.readTree(
                                "{\"string\":\"I am a"
                                    + " string\",\"integer\":1,\"lo\":2,\"bool\":true,\"flo\":0.1,\"doub\":0.2}"));
        var objValue = node.getObjectValue(TestObject::createFromDiscriminatorValue);
        assertEquals(1, objValue.getInteger());
        assertEquals(2L, objValue.getLo());
        assertEquals("I am a string", objValue.getString());
        assertEquals(true, objValue.getBool());
        assertEquals(0.1f, objValue.getFlo());
        assertEquals(0.2, objValue.getDoub());
    }

    @Test
    public void deserializeObjectTypeWithUnknownInt() throws IOException {
        // Arrange
        JsonParseNode node =
                new JsonParseNode(
                        mapper.readTree(
                                "{\"string\":\"I am a string\",\"integer\":1,\"unknownint\":2}"));
        var objValue = node.getObjectValue(TestObject::createFromDiscriminatorValue);
        assertEquals(2, objValue.getAdditionalData().get("unknownint"));
    }

    @Test
    public void deserializeObjectTypeWithUnknownString() throws IOException {
        // Arrange
        JsonParseNode node =
                new JsonParseNode(
                        mapper.readTree(
                                "{\"string\":\"I am a"
                                        + " string\",\"integer\":1,\"unknownstr\":\"unknown\"}"));
        var objValue = node.getObjectValue(TestObject::createFromDiscriminatorValue);
        assertEquals("unknown", objValue.getAdditionalData().get("unknownstr"));
    }

    @Test
    public void deserializeObjectTypeWithUnknownFloat() throws IOException {
        // Arrange
        JsonParseNode node =
                new JsonParseNode(
                        mapper.readTree(
                                "{\"string\":\"I am a"
                                        + " string\",\"integer\":1,\"unknownfloat\":0.1}"));
        var objValue = node.getObjectValue(TestObject::createFromDiscriminatorValue);
        assertEquals(0.1, objValue.getAdditionalData().get("unknownfloat"));
    }

    @Test
    public void deserializeObjectTypeWithUnknownBoolean() throws IOException {
        // Arrange
        JsonParseNode node =
                new JsonParseNode(
                        mapper.readTree(
                                "{\"string\":\"I am a"
                                        + " string\",\"integer\":1,\"unknownbool\":true}"));
        var objValue = node.getObjectValue(TestObject::createFromDiscriminatorValue);
        assertEquals(true, objValue.getAdditionalData().get("unknownbool"));
    }
}
