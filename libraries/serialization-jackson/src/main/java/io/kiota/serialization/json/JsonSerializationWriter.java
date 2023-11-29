package io.kiota.serialization.json;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.microsoft.kiota.PeriodAndDuration;
import com.microsoft.kiota.serialization.ComposedTypeWrapper;
import com.microsoft.kiota.serialization.Parsable;
import com.microsoft.kiota.serialization.SerializationWriter;
import com.microsoft.kiota.serialization.ValuedEnum;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Serialization writer implementation for JSON */
public class JsonSerializationWriter implements SerializationWriter {
    private final ByteArrayOutputStream stream = new ByteArrayOutputStream();
    private final JsonGenerator generator;

    /** Creates a new instance of a json serialization writer */
    public JsonSerializationWriter() {
        try {
            this.generator =
                    JsonMapper.mapper.writer().createGenerator(this.stream, JsonEncoding.UTF8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeStringValue(@Nullable final String key, @Nullable final String value) {
        if (value != null)
            try {
                if (key != null && !key.isEmpty()) {
                    generator.writeStringField(key, value);
                } else {
                    generator.writeString(value);
                }
            } catch (IOException ex) {
                throw new RuntimeException("could not serialize value", ex);
            }
    }

    public void writeBooleanValue(@Nullable final String key, @Nullable final Boolean value) {
        if (value != null)
            try {
                if (key != null && !key.isEmpty()) {
                    generator.writeBooleanField(key, value);
                } else {
                    generator.writeBoolean(value);
                }
            } catch (IOException ex) {
                throw new RuntimeException("could not serialize value", ex);
            }
    }

    public void writeShortValue(@Nullable final String key, @Nullable final Short value) {
        if (value != null)
            try {
                if (key != null && !key.isEmpty()) {
                    generator.writeNumberField(key, value);
                } else {
                    generator.writeNumber(value);
                }
            } catch (IOException ex) {
                throw new RuntimeException("could not serialize value", ex);
            }
    }

    public void writeByteValue(@Nullable final String key, @Nullable final Byte value) {
        if (value != null)
            try {
                if (key != null && !key.isEmpty()) {
                    generator.writeNumberField(key, value);
                } else {
                    generator.writeNumber(value);
                }
            } catch (IOException ex) {
                throw new RuntimeException("could not serialize value", ex);
            }
    }

    public void writeBigDecimalValue(@Nullable final String key, @Nullable final BigDecimal value) {
        if (value != null)
            try {
                if (key != null && !key.isEmpty()) {
                    generator.writeNumberField(key, value);
                } else {
                    generator.writeNumber(value);
                }
            } catch (IOException ex) {
                throw new RuntimeException("could not serialize value", ex);
            }
    }

    public void writeIntegerValue(@Nullable final String key, @Nullable final Integer value) {
        if (value != null)
            try {
                if (key != null && !key.isEmpty()) {
                    generator.writeNumberField(key, value);
                } else {
                    generator.writeNumber(value);
                }
            } catch (IOException ex) {
                throw new RuntimeException("could not serialize value", ex);
            }
    }

    public void writeFloatValue(@Nullable final String key, @Nullable final Float value) {
        if (value != null)
            try {
                if (key != null && !key.isEmpty()) {
                    generator.writeNumberField(key, value);
                } else {
                    generator.writeNumber(value);
                }
            } catch (IOException ex) {
                throw new RuntimeException("could not serialize value", ex);
            }
    }

    public void writeDoubleValue(@Nullable final String key, @Nullable final Double value) {
        if (value != null)
            try {
                if (key != null && !key.isEmpty()) {
                    generator.writeNumberField(key, value);
                } else {
                    generator.writeNumber(value);
                }
            } catch (IOException ex) {
                throw new RuntimeException("could not serialize value", ex);
            }
    }

    public void writeLongValue(@Nullable final String key, @Nullable final Long value) {
        if (value != null)
            try {
                if (key != null && !key.isEmpty()) {
                    generator.writeNumberField(key, value);
                } else {
                    generator.writeNumber(value);
                }
            } catch (IOException ex) {
                throw new RuntimeException("could not serialize value", ex);
            }
    }

    public void writeUUIDValue(@Nullable final String key, @Nullable final UUID value) {
        if (value != null)
            try {
                if (key != null && !key.isEmpty()) {
                    generator.writeStringField(key, value.toString());
                } else {
                    generator.writeString(value.toString());
                }
            } catch (IOException ex) {
                throw new RuntimeException("could not serialize value", ex);
            }
    }

    public void writeOffsetDateTimeValue(
            @Nullable final String key, @Nullable final OffsetDateTime value) {
        if (value != null)
            try {
                String date = value.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
                if (key != null && !key.isEmpty()) {
                    generator.writeStringField(key, date);
                } else {
                    generator.writeString(date);
                }
            } catch (IOException ex) {
                throw new RuntimeException("could not serialize value", ex);
            }
    }

    public void writeLocalDateValue(@Nullable final String key, @Nullable final LocalDate value) {
        if (value != null)
            try {
                String date = value.format(DateTimeFormatter.ISO_LOCAL_DATE);
                if (key != null && !key.isEmpty()) {
                    generator.writeStringField(key, date);
                } else {
                    generator.writeString(date);
                }
            } catch (IOException ex) {
                throw new RuntimeException("could not serialize value", ex);
            }
    }

    public void writeLocalTimeValue(@Nullable final String key, @Nullable final LocalTime value) {
        if (value != null)
            try {
                String date = value.format(DateTimeFormatter.ISO_LOCAL_TIME);
                if (key != null && !key.isEmpty()) {
                    generator.writeStringField(key, date);
                } else {
                    generator.writeString(date);
                }
            } catch (IOException ex) {
                throw new RuntimeException("could not serialize value", ex);
            }
    }

    public void writePeriodAndDurationValue(
            @Nullable final String key, @Nullable final PeriodAndDuration value) {
        if (value != null)
            try {
                if (key != null && !key.isEmpty()) {
                    generator.writeStringField(key, value.toString());
                } else {
                    generator.writeString(value.toString());
                }
            } catch (IOException ex) {
                throw new RuntimeException("could not serialize value", ex);
            }
    }

    public <T> void writeCollectionOfPrimitiveValues(
            @Nullable final String key, @Nullable final Iterable<T> values) {
        try {
            if (values != null) { // empty array is meaningful
                if (key != null && !key.isEmpty()) {
                    generator.writeArrayFieldStart(key);
                } else {
                    generator.writeStartArray();
                }
                for (final T t : values) {
                    this.writeAnyValue(null, t);
                }
                generator.writeEndArray();
            }
        } catch (IOException ex) {
            throw new RuntimeException("could not serialize value", ex);
        }
    }

    public <T extends Parsable> void writeCollectionOfObjectValues(
            @Nullable final String key, @Nullable final Iterable<T> values) {
        try {
            if (values != null) { // empty array is meaningful
                if (key != null && !key.isEmpty()) {
                    generator.writeArrayFieldStart(key);
                } else {
                    generator.writeStartArray();
                }
                for (final T t : values) {
                    this.writeObjectValue(null, t);
                }
                generator.writeEndArray();
            }
        } catch (IOException ex) {
            throw new RuntimeException("could not serialize value", ex);
        }
    }

    public <T extends Enum<T>> void writeCollectionOfEnumValues(
            @Nullable final String key, @Nullable final Iterable<T> values) {
        try {
            if (values != null) { // empty array is meaningful
                if (key != null && !key.isEmpty()) {
                    generator.writeArrayFieldStart(key);
                } else {
                    generator.writeStartArray();
                }
                for (final T t : values) {
                    this.writeEnumValue(null, t);
                }
                generator.writeEndArray();
            }
        } catch (IOException ex) {
            throw new RuntimeException("could not serialize value", ex);
        }
    }

    public <T extends Parsable> void writeObjectValue(
            @Nullable final String key,
            @Nullable final T value,
            @Nonnull final Parsable... additionalValuesToMerge) {
        Objects.requireNonNull(additionalValuesToMerge);
        try {
            final List<Parsable> nonNullAdditionalValuesToMerge =
                    Stream.of(additionalValuesToMerge)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
            if (value != null || !nonNullAdditionalValuesToMerge.isEmpty()) {
                if (key != null && !key.isEmpty()) {
                    generator.writeFieldName(key);
                }
                if (onBeforeObjectSerialization != null && value != null) {
                    onBeforeObjectSerialization.accept(value);
                }
                final boolean serializingComposedType = value instanceof ComposedTypeWrapper;
                if (!serializingComposedType) {
                    generator.writeStartObject();
                }
                if (value != null) {
                    if (onStartObjectSerialization != null) {
                        onStartObjectSerialization.accept(value, this);
                    }
                    value.serialize(this);
                }
                for (final Parsable additionalValueToMerge : nonNullAdditionalValuesToMerge) {
                    if (onBeforeObjectSerialization != null) {
                        onBeforeObjectSerialization.accept(additionalValueToMerge);
                    }
                    if (onStartObjectSerialization != null) {
                        onStartObjectSerialization.accept(additionalValueToMerge, this);
                    }
                    additionalValueToMerge.serialize(this);
                    if (onAfterObjectSerialization != null) {
                        onAfterObjectSerialization.accept(additionalValueToMerge);
                    }
                }
                if (!serializingComposedType) {
                    generator.writeEndObject();
                }
                if (onAfterObjectSerialization != null && value != null) {
                    onAfterObjectSerialization.accept(value);
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("could not serialize value", ex);
        }
    }

    public <T extends Enum<T>> void writeEnumSetValue(
            @Nullable final String key, @Nullable final EnumSet<T> values) {
        if (values != null && !values.isEmpty()) {
            final Optional<String> concatenatedValue =
                    values.stream()
                            .map(v -> this.getStringValueFromValuedEnum(v))
                            .reduce(
                                    (x, y) -> {
                                        return x + "," + y;
                                    });
            if (concatenatedValue.isPresent()) {
                this.writeStringValue(key, concatenatedValue.get());
            }
        }
    }

    public <T extends Enum<T>> void writeEnumValue(
            @Nullable final String key, @Nullable final T value) {
        if (value != null) {
            this.writeStringValue(key, getStringValueFromValuedEnum(value));
        }
    }

    public void writeNullValue(@Nullable final String key) {
        try {
            if (key != null && !key.isEmpty()) {
                generator.writeNullField(key);
            } else {
                generator.writeNull();
            }
        } catch (IOException ex) {
            throw new RuntimeException("could not serialize value", ex);
        }
    }

    private <T extends Enum<T>> String getStringValueFromValuedEnum(final T value) {
        if (value instanceof ValuedEnum) {
            final ValuedEnum valued = (ValuedEnum) value;
            return valued.getValue();
        } else return null;
    }

    @Nonnull
    public InputStream getSerializedContent() {
        try {
            this.generator.flush();
            return new ByteArrayInputStream(this.stream.toByteArray());
            // This copies the whole array in memory could result in memory pressure for large
            // objects, we might want to replace by some kind of piping in the future
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void close() throws IOException {
        if (this.generator != null && !this.generator.isClosed()) {
            this.generator.close();
        }
        this.stream.close();
    }

    public void writeAdditionalData(@Nonnull final Map<String, Object> value) {
        if (value == null) return;
        for (final Map.Entry<String, Object> dataValue : value.entrySet()) {
            this.writeAnyValue(dataValue.getKey(), dataValue.getValue());
        }
    }

    private void writeNonParsableObject(@Nullable final String key, @Nullable final Object value) {
        try {
            if (value == null) writeNullValue(key);
            else {
                if (key != null && !key.isEmpty()) {
                    generator.writeObjectFieldStart(key);
                } else {
                    generator.writeStartObject();
                }
                final Class<?> valueClass = value.getClass();
                for (final Field oProp : valueClass.getFields())
                    writeAnyValue(oProp.getName(), oProp.get(value));
                generator.writeEndObject();
            }
        } catch (IOException | IllegalAccessException ex) {
            throw new RuntimeException("could not serialize value", ex);
        }
    }

    private void writeAnyValue(@Nullable final String key, @Nullable final Object value) {
        if (value == null) {
            this.writeNullValue(key);
        } else {
            final Class<?> valueClass = value.getClass();
            if (valueClass.equals(String.class)) this.writeStringValue(key, (String) value);
            else if (valueClass.equals(Boolean.class)) this.writeBooleanValue(key, (Boolean) value);
            else if (valueClass.equals(Byte.class)) this.writeByteValue(key, (Byte) value);
            else if (valueClass.equals(Short.class)) this.writeShortValue(key, (Short) value);
            else if (valueClass.equals(BigDecimal.class))
                this.writeBigDecimalValue(key, (BigDecimal) value);
            else if (valueClass.equals(Float.class)) this.writeFloatValue(key, (Float) value);
            else if (valueClass.equals(Long.class)) this.writeLongValue(key, (Long) value);
            else if (valueClass.equals(Integer.class)) this.writeIntegerValue(key, (Integer) value);
            else if (valueClass.equals(UUID.class)) this.writeUUIDValue(key, (UUID) value);
            else if (valueClass.equals(OffsetDateTime.class))
                this.writeOffsetDateTimeValue(key, (OffsetDateTime) value);
            else if (valueClass.equals(LocalDate.class))
                this.writeLocalDateValue(key, (LocalDate) value);
            else if (valueClass.equals(LocalTime.class))
                this.writeLocalTimeValue(key, (LocalTime) value);
            else if (valueClass.equals(PeriodAndDuration.class))
                this.writePeriodAndDurationValue(key, (PeriodAndDuration) value);
            else if (value instanceof Iterable<?>)
                this.writeCollectionOfPrimitiveValues(key, (Iterable<?>) value);
            else if (!valueClass.isPrimitive()) this.writeNonParsableObject(key, value);
            else throw new RuntimeException("unknown type to serialize " + valueClass.getName());
        }
    }

    @Nullable
    public Consumer<Parsable> getOnBeforeObjectSerialization() {
        return this.onBeforeObjectSerialization;
    }

    @Nullable
    public Consumer<Parsable> getOnAfterObjectSerialization() {
        return this.onAfterObjectSerialization;
    }

    @Nullable
    public BiConsumer<Parsable, SerializationWriter> getOnStartObjectSerialization() {
        return this.onStartObjectSerialization;
    }

    private Consumer<Parsable> onBeforeObjectSerialization;

    public void setOnBeforeObjectSerialization(@Nullable final Consumer<Parsable> value) {
        this.onBeforeObjectSerialization = value;
    }

    private Consumer<Parsable> onAfterObjectSerialization;

    public void setOnAfterObjectSerialization(@Nullable final Consumer<Parsable> value) {
        this.onAfterObjectSerialization = value;
    }

    private BiConsumer<Parsable, SerializationWriter> onStartObjectSerialization;

    public void setOnStartObjectSerialization(
            @Nullable final BiConsumer<Parsable, SerializationWriter> value) {
        this.onStartObjectSerialization = value;
    }

    public void writeByteArrayValue(@Nullable final String key, @Nullable final byte[] value) {
        if (value != null) this.writeStringValue(key, Base64.getEncoder().encodeToString(value));
    }
}
