package com.redhat.cloud.kiota.serialization.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import com.microsoft.kiota.serialization.AdditionalDataHolder;
import com.microsoft.kiota.serialization.Parsable;
import com.microsoft.kiota.serialization.ParsableFactory;
import com.microsoft.kiota.serialization.ParseNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public class JacksonJsonParseNode implements ParseNode {

    private final JsonNode currentNode;

    JacksonJsonParseNode(JsonNode currentNode) {
        this.currentNode = currentNode;
    }

    @Nullable
    @Override
    public ParseNode getChildNode(@Nonnull String identifier) {
        Objects.requireNonNull(identifier, "identifier parameter is required");

        if (currentNode.isObject()) {
            final Consumer<Parsable> onBefore = this.onBeforeAssignFieldValues;
            final Consumer<Parsable> onAfter = this.onAfterAssignFieldValues;
            return new JacksonJsonParseNode(currentNode.get(identifier)) {{
                this.setOnBeforeAssignFieldValues(onBefore);
                this.setOnAfterAssignFieldValues(onAfter);
            }};
        } else return null;
    }

    @Nullable
    @Override
    public String getStringValue() {
        return currentNode.isTextual() ? currentNode.textValue() : null;
    }

    @Nullable
    @Override
    public Boolean getBooleanValue() {
        return currentNode.isBoolean() ? currentNode.booleanValue() : null;
    }

    @Nullable
    @Override
    public Byte getByteValue() {
        return currentNode.isShort() ? (byte) currentNode.shortValue() : null; // Todo: Is this Ok?
    }

    @Nullable
    @Override
    public Short getShortValue() {
        return currentNode.isShort() ? currentNode.shortValue() : null;
    }

    @Nullable
    @Override
    public BigDecimal getBigDecimalValue() {
        return currentNode.isBigDecimal() ? currentNode.decimalValue() : null;
    }

    @Nullable
    @Override
    public Integer getIntegerValue() {
        return currentNode.isInt() ? currentNode.intValue() : null;
    }

    @Nullable
    @Override
    public Float getFloatValue() {
        return currentNode.isFloat() ? currentNode.floatValue() : null;
    }

    @Nullable
    @Override
    public Double getDoubleValue() {
        return currentNode.isDouble() ? currentNode.doubleValue() : null;
    }

    @Nullable
    @Override
    public Long getLongValue() {
        return currentNode.isLong() ? currentNode.longValue() : null;
    }

    @Nullable
    @Override
    public UUID getUUIDValue() {
        final String stringValue = getStringValue();
        if (stringValue == null) {
            return null;
        }
        return UUID.fromString(stringValue);
    }

    @Nullable
    @Override
    public OffsetDateTime getOffsetDateTimeValue() {
        final String stringValue = getStringValue();
        if (stringValue == null) return null;
        return OffsetDateTime.parse(stringValue);
    }

    @Nullable
    @Override
    public LocalDate getLocalDateValue() {
        final String stringValue = getStringValue();
        if (stringValue == null) return null;
        return LocalDate.parse(stringValue);
    }

    @Nullable
    @Override
    public LocalTime getLocalTimeValue() {
        final String stringValue = getStringValue();
        if (stringValue == null) return null;
        return LocalTime.parse(stringValue);
    }

    @Nullable
    @Override
    public Period getPeriodValue() {
        final String stringValue = getStringValue();
        if (stringValue == null) return null;
        return Period.parse(stringValue);
    }

    @Nullable
    @Override
    public <T extends Enum<T>> T getEnumValue(@Nonnull Class<T> targetEnum) {
        final String rawValue = this.getStringValue();
        if (rawValue == null || rawValue.isEmpty()) {
            return null;
        }
        return getEnumValueInt(rawValue, targetEnum);
    }

    @SuppressWarnings("unchecked")
    private <T extends Enum<T>> T getEnumValueInt(@Nonnull final String rawValue, @Nonnull final Class<T> targetEnum) {
        try {
            return (T) targetEnum.getMethod("forValue", String.class).invoke(null, rawValue);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | SecurityException ex) {
            return null;
        }
    }

    @Nullable
    @Override
    public <T extends Enum<T>> EnumSet<T> getEnumSetValue(@Nonnull Class<T> targetEnum) {
        final String rawValue = this.getStringValue();
        if (rawValue == null || rawValue.isEmpty()) {
            return null;
        }
        final EnumSet<T> result = EnumSet.noneOf(targetEnum);
        final String[] rawValues = rawValue.split(",");
        for (final String rawValueItem : rawValues) {
            final T value = getEnumValueInt(rawValueItem, targetEnum);
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    @Nullable
    @Override
    public <T> List<T> getCollectionOfPrimitiveValues(@Nonnull Class<T> targetClass) {
        Objects.requireNonNull(targetClass, "parameter targetClass cannot be null");
        if (currentNode.isNull()) {
            return null;
        } else if (currentNode.isArray()) {
            final ArrayNode arrayNode = (ArrayNode) currentNode;
            final Iterator<JsonNode> sourceIterator = arrayNode.iterator();
            final JacksonJsonParseNode _this = this;
            return Lists.newArrayList(new Iterable<T>() {
                @Override
                public Iterator<T> iterator() {
                    return new Iterator<T>() {
                        @Override
                        public boolean hasNext() {
                            return sourceIterator.hasNext();
                        }

                        @Override
                        @SuppressWarnings("unchecked")
                        public T next() {
                            final JsonNode item = sourceIterator.next();
                            final Consumer<Parsable> onBefore = _this.getOnBeforeAssignFieldValues();
                            final Consumer<Parsable> onAfter = _this.getOnAfterAssignFieldValues();
                            final JacksonJsonParseNode itemNode = new JacksonJsonParseNode(item) {{
                                this.setOnBeforeAssignFieldValues(onBefore);
                                this.setOnAfterAssignFieldValues(onAfter);
                            }};
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
                            } else if (targetClass == Period.class) {
                                return (T) itemNode.getPeriodValue();
                            } else {
                                throw new RuntimeException("unknown type to deserialize " + targetClass.getName());
                            }
                        }
                    };
                }
            });
        } else {
            throw new RuntimeException("invalid state expected to have an array node");
        }
    }

    @Nullable
    @Override
    public <T extends Parsable> List<T> getCollectionOfObjectValues(@Nonnull ParsableFactory<T> factory) {
        Objects.requireNonNull(factory, "parameter factory cannot be null");
        if (currentNode.isNull()) {
            return null;
        } else if (currentNode.isArray()) {
            final ArrayNode arrayNode = (ArrayNode) currentNode;
            final Iterator<JsonNode> sourceIterator = arrayNode.iterator();
            final JacksonJsonParseNode _this = this;
            return Lists.newArrayList(new Iterable<T>() {
                @Override
                public Iterator<T> iterator() {
                    return new Iterator<T>() {
                        @Override
                        public boolean hasNext() {
                            return sourceIterator.hasNext();
                        }

                        @Override
                        public T next() {
                            final JsonNode item = sourceIterator.next();
                            final Consumer<Parsable> onBefore = _this.getOnBeforeAssignFieldValues();
                            final Consumer<Parsable> onAfter = _this.getOnAfterAssignFieldValues();
                            final JacksonJsonParseNode itemNode = new JacksonJsonParseNode(item) {{
                                this.setOnBeforeAssignFieldValues(onBefore);
                                this.setOnAfterAssignFieldValues(onAfter);
                            }};
                            return itemNode.getObjectValue(factory);
                        }
                    };
                }

            });
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    public <T extends Enum<T>> List<T> getCollectionOfEnumValues(@Nonnull Class<T> targetEnum) {
        Objects.requireNonNull(targetEnum, "parameter targetEnum cannot be null");
        if (currentNode.isNull()) {
            return null;
        } else if (currentNode.isArray()) {
            final ArrayNode arrayNode = (ArrayNode) currentNode;
            final Iterator<JsonNode> sourceIterator = arrayNode.iterator();
            final JacksonJsonParseNode _this = this;
            return Lists.newArrayList(new Iterable<T>() {
                @Override
                public Iterator<T> iterator() {
                    return new Iterator<T>() {
                        @Override
                        public boolean hasNext() {
                            return sourceIterator.hasNext();
                        }

                        @Override
                        public T next() {
                            final JsonNode item = sourceIterator.next();
                            final Consumer<Parsable> onBefore = _this.getOnBeforeAssignFieldValues();
                            final Consumer<Parsable> onAfter = _this.getOnAfterAssignFieldValues();
                            final JacksonJsonParseNode itemNode = new JacksonJsonParseNode(item) {{
                                this.setOnBeforeAssignFieldValues(onBefore);
                                this.setOnAfterAssignFieldValues(onAfter);
                            }};
                            return itemNode.getEnumValue(targetEnum);
                        }
                    };
                }

            });
        } else throw new RuntimeException("invalid state expected to have an array node");
    }

    @Nonnull
    @Override
    public <T extends Parsable> T getObjectValue(@Nonnull ParsableFactory<T> factory) {
        Objects.requireNonNull(factory, "parameter factory cannot be null");
        final T item = factory.create(this);
        assignFieldValues(item, item.getFieldDeserializers());
        return item;
    }

    private <T extends Parsable> void assignFieldValues(final T item, final Map<String, Consumer<ParseNode>> fieldDeserializers) {
        if (currentNode.isObject()) {
            if (this.onBeforeAssignFieldValues != null) {
                this.onBeforeAssignFieldValues.accept(item);
            }
            Map<String, Object> itemAdditionalData = null;
            if (item instanceof AdditionalDataHolder) {
                itemAdditionalData = ((AdditionalDataHolder) item).getAdditionalData();
            }
            Iterator<Map.Entry<String, JsonNode>> iterator = currentNode.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> fieldEntry = iterator.next();
                final String fieldKey = fieldEntry.getKey();
                final Consumer<ParseNode> fieldDeserializer = fieldDeserializers.get(fieldKey);
                final JsonNode fieldValue = fieldEntry.getValue();
                if (fieldValue.isNull())
                    continue;
                if (fieldDeserializer != null) {
                    final Consumer<Parsable> onBefore = this.onBeforeAssignFieldValues;
                    final Consumer<Parsable> onAfter = this.onAfterAssignFieldValues;
                    fieldDeserializer.accept(new JacksonJsonParseNode(fieldValue) {{
                        this.setOnBeforeAssignFieldValues(onBefore);
                        this.setOnAfterAssignFieldValues(onAfter);
                    }});
                } else if (itemAdditionalData != null)
                    itemAdditionalData.put(fieldKey, this.tryGetAnything(fieldValue));
            }
            if (this.onAfterAssignFieldValues != null) {
                this.onAfterAssignFieldValues.accept(item);
            }
        }
    }

    private Object tryGetAnything(final JsonNode element) {
        if (element.isBoolean()) {
            return element.booleanValue();
        } else if (element.isTextual()) {
            return element.textValue();
        } else if (element.isNumber()) {
            return element.floatValue();
        } else if (element.isNull())
            return null;
        else if (element.isObject() || element.isArray())
            return element;
        else
            throw new RuntimeException("Could not get the value during deserialization, unknown primitive type");
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
    @Override
    public byte[] getByteArrayValue() {
        final String base64 = this.getStringValue();
        if (base64 == null || base64.isEmpty()) {
            return null;
        }
        return Base64.getDecoder().decode(base64);
    }
}
