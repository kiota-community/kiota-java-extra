package io.kiota.serialization.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.kiota.PeriodAndDuration;
import com.microsoft.kiota.serialization.AdditionalDataHolder;
import com.microsoft.kiota.serialization.Parsable;
import com.microsoft.kiota.serialization.ParsableFactory;
import com.microsoft.kiota.serialization.ParseNode;
import com.microsoft.kiota.serialization.ValuedEnumParser;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/** ParseNode implementation for JSON */
public class JsonParseNode implements ParseNode {
    private final JsonParseNodeFactory factory;
    protected final JsonNode currentNode;

    /**
     * Creates a new instance of the JsonParseNode class.
     * @param node the node to wrap.
     */
    public JsonParseNode(
            @Nonnull final JsonParseNodeFactory nodeFactory, @Nonnull final JsonNode node) {
        factory = Objects.requireNonNull(nodeFactory, "parameter nodeFactory cannot be null");
        currentNode = Objects.requireNonNull(node, "parameter node cannot be null");
    }

    /** {@inheritDoc} */
    @Nullable
    public ParseNode getChildNode(@Nonnull final String identifier) {
        Objects.requireNonNull(identifier, "identifier parameter is required");
        if (currentNode.isObject()) {
            final Consumer<Parsable> onBefore = this.onBeforeAssignFieldValues;
            final Consumer<Parsable> onAfter = this.onAfterAssignFieldValues;
            final JsonParseNode node = factory.createJsonParseNode(currentNode.get(identifier));
            node.setOnBeforeAssignFieldValues(onBefore);
            node.setOnAfterAssignFieldValues(onAfter);
            return node;
        } else return null;
    }

    @Nullable
    public String getStringValue() {
        return currentNode.isTextual() ? currentNode.textValue() : null;
    }

    @Nullable
    public Boolean getBooleanValue() {
        return currentNode.isBoolean() ? currentNode.booleanValue() : null;
    }

    @Nullable
    public Byte getByteValue() {
        if (currentNode.isInt()) {
            int intValue = currentNode.intValue();
            if (intValue >= Byte.MIN_VALUE && intValue <= Byte.MAX_VALUE) {
                return Integer.valueOf(intValue).byteValue();
            }
        }
        return null;
    }

    @Nullable
    public Short getShortValue() {
        return currentNode.canConvertToInt() ? currentNode.shortValue() : null;
    }

    @Nullable
    public BigDecimal getBigDecimalValue() {
        return currentNode.isNumber() ? currentNode.decimalValue() : null;
    }

    @Nullable
    public Integer getIntegerValue() {
        return currentNode.canConvertToInt() ? currentNode.intValue() : null;
    }

    @Nullable
    public Float getFloatValue() {
        if (currentNode.isNumber()) {
            double doubleValue = currentNode.doubleValue();
            if (doubleValue >= Float.MIN_VALUE && doubleValue <= Float.MAX_VALUE) {
                return Double.valueOf(doubleValue).floatValue();
            }
        }
        return null;
    }

    @Nullable
    public Double getDoubleValue() {
        return currentNode.isNumber() ? currentNode.doubleValue() : null;
    }

    @Nullable
    public Long getLongValue() {
        return currentNode.canConvertToLong() ? currentNode.longValue() : null;
    }

    @Nullable
    public UUID getUUIDValue() {
        if (currentNode.isTextual() && !currentNode.isNull()) {
            return UUID.fromString(currentNode.textValue());
        }
        return null;
    }

    @Nullable
    public OffsetDateTime getOffsetDateTimeValue() {
        if (currentNode.isTextual() && !currentNode.isNull()) {
            return OffsetDateTime.parse(currentNode.textValue());
        }
        return null;
    }

    @Nullable
    public LocalDate getLocalDateValue() {
        if (currentNode.isTextual() && !currentNode.isNull()) {
            return LocalDate.parse(currentNode.textValue());
        }
        return null;
    }

    @Nullable
    public LocalTime getLocalTimeValue() {
        if (currentNode.isTextual() && !currentNode.isNull()) {
            return LocalTime.parse(currentNode.textValue());
        }
        return null;
    }

    @Nullable
    public PeriodAndDuration getPeriodAndDurationValue() {
        if (currentNode.isTextual() && !currentNode.isNull()) {
            return PeriodAndDuration.parse(currentNode.textValue());
        }
        return null;
    }

    @Nullable
    private <T> T getPrimitiveElement(
            @Nonnull final Class<T> targetClass, @Nonnull final JsonParseNode itemNode) {
        if (targetClass == Boolean.class) {
            return (T) itemNode.getBooleanValue();
        } else if (targetClass == Short.class) {
            return (T) itemNode.getShortValue();
        } else if (targetClass == Byte.class) {
            return (T) itemNode.getByteValue();
        } else if (targetClass == BigDecimal.class) {
            return (T) itemNode.getBigDecimalValue();
        } else if (targetClass == String.class) {
            return (T) itemNode.getStringValue();
        } else if (targetClass == Integer.class) {
            return (T) itemNode.getIntegerValue();
        } else if (targetClass == Float.class) {
            return (T) itemNode.getFloatValue();
        } else if (targetClass == Long.class) {
            return (T) itemNode.getLongValue();
        } else if (targetClass == UUID.class) {
            return (T) itemNode.getUUIDValue();
        } else if (targetClass == OffsetDateTime.class) {
            return (T) itemNode.getOffsetDateTimeValue();
        } else if (targetClass == LocalDate.class) {
            return (T) itemNode.getLocalDateValue();
        } else if (targetClass == LocalTime.class) {
            return (T) itemNode.getLocalTimeValue();
        } else if (targetClass == PeriodAndDuration.class) {
            return (T) itemNode.getPeriodAndDurationValue();
        } else {
            throw new RuntimeException("unknown type to deserialize " + targetClass.getName());
        }
    }

    @Nullable
    public <T> List<T> getCollectionOfPrimitiveValues(@Nonnull final Class<T> targetClass) {
        Objects.requireNonNull(targetClass, "parameter targetClass cannot be null");
        if (currentNode.isNull()) {
            return null;
        } else if (currentNode.isArray()) {
            Iterator<JsonNode> iter = currentNode.elements();
            List<T> result = new ArrayList<>();
            while (iter.hasNext()) {
                JsonNode item = iter.next();
                final JsonParseNode itemNode = factory.createJsonParseNode(item);
                itemNode.setOnBeforeAssignFieldValues(this.getOnBeforeAssignFieldValues());
                itemNode.setOnAfterAssignFieldValues(this.getOnAfterAssignFieldValues());
                result.add(getPrimitiveElement(targetClass, itemNode));
            }
            return result;
        } else throw new RuntimeException("invalid state expected to have an array node");
    }

    @Nullable
    public <T extends Parsable> List<T> getCollectionOfObjectValues(
            @Nonnull final ParsableFactory<T> factory) {
        Objects.requireNonNull(factory, "parameter factory cannot be null");
        if (currentNode.isNull()) {
            return null;
        } else if (currentNode.isArray()) {
            Iterator<JsonNode> iter = currentNode.elements();
            List<T> result = new ArrayList<>();
            while (iter.hasNext()) {
                JsonNode item = iter.next();
                final JsonParseNode itemNode = this.factory.createJsonParseNode(item);
                itemNode.setOnBeforeAssignFieldValues(this.getOnBeforeAssignFieldValues());
                itemNode.setOnAfterAssignFieldValues(this.getOnAfterAssignFieldValues());
                result.add(itemNode.getObjectValue(factory));
            }
            return result;
        } else return null;
    }

    @Nullable
    public <T extends Enum<T>> List<T> getCollectionOfEnumValues(
            @Nonnull final ValuedEnumParser<T> enumParser) {
        Objects.requireNonNull(enumParser, "parameter enumParser cannot be null");
        if (currentNode.isNull()) {
            return null;
        } else if (currentNode.isArray()) {
            Iterator<JsonNode> iter = currentNode.elements();
            List<T> result = new ArrayList<>();
            while (iter.hasNext()) {
                JsonNode item = iter.next();
                final JsonParseNode itemNode = factory.createJsonParseNode(item);
                itemNode.setOnBeforeAssignFieldValues(this.getOnBeforeAssignFieldValues());
                itemNode.setOnAfterAssignFieldValues(this.getOnAfterAssignFieldValues());
                result.add(itemNode.getEnumValue(enumParser));
            }
            return result;
        } else throw new RuntimeException("invalid state expected to have an array node");
    }

    @Nonnull
    public <T extends Parsable> T getObjectValue(@Nonnull final ParsableFactory<T> factory) {
        Objects.requireNonNull(factory, "parameter factory cannot be null");
        final T item = factory.create(this);
        assignFieldValues(item, item.getFieldDeserializers());
        return item;
    }

    @Nullable
    public <T extends Enum<T>> T getEnumValue(@Nonnull final ValuedEnumParser<T> enumParser) {
        final String rawValue = this.getStringValue();
        if (rawValue == null || rawValue.isEmpty()) {
            return null;
        }
        return enumParser.forValue(rawValue);
    }

    @Nullable
    public <T extends Enum<T>> EnumSet<T> getEnumSetValue(
            @Nonnull final ValuedEnumParser<T> enumParser) {
        final String rawValue = this.getStringValue();
        if (rawValue == null || rawValue.isEmpty()) {
            return null;
        }
        final List<T> result = new ArrayList<>();
        final String[] rawValues = rawValue.split(",");
        for (final String rawValueItem : rawValues) {
            final T value = enumParser.forValue(rawValueItem);
            if (value != null) {
                result.add(value);
            }
        }
        return EnumSet.copyOf(result);
    }

    private <T extends Parsable> void assignFieldValues(
            final T item, final Map<String, Consumer<ParseNode>> fieldDeserializers) {
        if (currentNode.isObject()) {
            if (this.onBeforeAssignFieldValues != null) {
                this.onBeforeAssignFieldValues.accept(item);
            }
            Map<String, Object> itemAdditionalData = null;
            if (item instanceof AdditionalDataHolder) {
                itemAdditionalData = ((AdditionalDataHolder) item).getAdditionalData();
            }
            Iterator<Map.Entry<String, JsonNode>> iter = currentNode.fields();
            while (iter.hasNext()) {
                Map.Entry<String, JsonNode> fieldEntry = iter.next();
                final String fieldKey = fieldEntry.getKey();
                final Consumer<ParseNode> fieldDeserializer = fieldDeserializers.get(fieldKey);
                final JsonNode fieldValue = fieldEntry.getValue();
                if (fieldValue.isNull()) continue;
                if (fieldDeserializer != null) {
                    JsonParseNode itemNode = factory.createJsonParseNode(fieldValue);
                    itemNode.setOnBeforeAssignFieldValues(this.onBeforeAssignFieldValues);
                    itemNode.setOnAfterAssignFieldValues(this.onAfterAssignFieldValues);
                    fieldDeserializer.accept(itemNode);
                } else if (itemAdditionalData != null)
                    itemAdditionalData.put(fieldKey, this.tryGetAnything(fieldValue));
            }
            if (this.onAfterAssignFieldValues != null) {
                this.onAfterAssignFieldValues.accept(item);
            }
        }
    }

    private Object tryGetAnything(final JsonNode element) {
        if (element.isNull()) return null;
        else if (element.isValueNode()) {
            if (element.isBoolean()) return element.booleanValue();
            else if (element.isTextual()) return element.textValue();
            else if (element.isInt()) return element.intValue();
            else if (element.isLong()) return element.longValue();
            else if (element.isFloatingPointNumber() && element.isFloat())
                return element.floatValue();
            else if (element.isFloatingPointNumber() && element.isDouble())
                return element.doubleValue();
            else
                throw new RuntimeException(
                        "Could not get the value during deserialization, unknown primitive type");
        } else if (element.isObject() || element.isArray()) return element;
        else {
            throw new RuntimeException(
                    "Could not get the value during deserialization, unknown primitive type");
        }
    }

    @Nullable
    public Consumer<Parsable> getOnBeforeAssignFieldValues() {
        return this.onBeforeAssignFieldValues;
    }

    @Nullable
    public Consumer<Parsable> getOnAfterAssignFieldValues() {
        return this.onAfterAssignFieldValues;
    }

    private Consumer<Parsable> onBeforeAssignFieldValues;

    public void setOnBeforeAssignFieldValues(@Nullable final Consumer<Parsable> value) {
        this.onBeforeAssignFieldValues = value;
    }

    private Consumer<Parsable> onAfterAssignFieldValues;

    public void setOnAfterAssignFieldValues(@Nullable final Consumer<Parsable> value) {
        this.onAfterAssignFieldValues = value;
    }

    @Nullable
    public byte[] getByteArrayValue() {
        final String base64 = this.getStringValue();
        if (base64 == null || base64.isEmpty()) {
            return null;
        }
        return Base64.getDecoder().decode(base64);
    }
}
