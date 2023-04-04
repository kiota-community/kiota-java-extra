package com.redhat.cloud.kiota.serialization.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.microsoft.kiota.serialization.Parsable;
import com.microsoft.kiota.serialization.SerializationWriter;
import com.microsoft.kiota.serialization.ValuedEnum;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;
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

public class JacksonJsonSerializationWriter implements SerializationWriter {

    private final ByteArrayOutputStream stream = new ByteArrayOutputStream();
    private final JsonGenerator jsonGenerator;

    private Consumer<Parsable> onBeforeObjectSerialization = null;
    private BiConsumer<Parsable, SerializationWriter> onStartObjectSerialization = null;
    private Consumer<Parsable> onAfterObjectSerialization = null;

    JacksonJsonSerializationWriter() {
        try {
            JsonFactory jsonFactory = new JsonFactory();
            jsonGenerator = jsonFactory.createGenerator(new OutputStreamWriter(this.stream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("could not create json writer", e);
        }
    }

    @Override
    public void writeStringValue(@Nullable String key, @Nullable String value) {
        if (value != null) {
            try {
                if (key != null && !key.isEmpty()) {
                    jsonGenerator.writeFieldName(key);
                }
                jsonGenerator.writeString(value);
            } catch (IOException ex) {
                throw new RuntimeException("could not serialize value", ex);
            }
        }
    }

    @Override
    public void writeBooleanValue(@Nullable String key, @Nullable Boolean value) {
        if (value != null) {
            try {
                if (key != null && !key.isEmpty()) {
                    jsonGenerator.writeFieldName(key);
                }
                jsonGenerator.writeBoolean(value);
            } catch (IOException ex) {
                throw new RuntimeException("could not serialize value", ex);
            }
        }
    }

    @Override
    public void writeByteValue(@Nullable String key, @Nullable Byte value) {
        if (value != null) {
            try {
                if (key != null && !key.isEmpty()) {
                    jsonGenerator.writeFieldName(key);
                }
                jsonGenerator.writeNumber(value); // TODO: Is this right?
            } catch (IOException ex) {
                throw new RuntimeException("could not serialize value", ex);
            }
        }
    }

    @Override
    public void writeShortValue(@Nullable String key, @Nullable Short value) {
        if (value != null) {
            try {
                if (key != null && !key.isEmpty()) {
                    jsonGenerator.writeFieldName(key);
                }
                jsonGenerator.writeNumber(value);
            } catch (IOException ex) {
                throw new RuntimeException("could not serialize value", ex);
            }
        }
    }

    @Override
    public void writeBigDecimalValue(@Nullable String key, @Nullable BigDecimal value) {
        if (value != null) {
            try {
                if (key != null && !key.isEmpty()) {
                    jsonGenerator.writeFieldName(key);
                }
                jsonGenerator.writeNumber(value);
            } catch (IOException ex) {
                throw new RuntimeException("could not serialize value", ex);
            }
        }
    }

    @Override
    public void writeIntegerValue(@Nullable String key, @Nullable Integer value) {
        if (value != null) {
            try {
                if (key != null && !key.isEmpty()) {
                    jsonGenerator.writeFieldName(key);
                }
                jsonGenerator.writeNumber(value);
            } catch (IOException ex) {
                throw new RuntimeException("could not serialize value", ex);
            }
        }
    }

    @Override
    public void writeFloatValue(@Nullable String key, @Nullable Float value) {
        if (value != null) {
            try {
                if (key != null && !key.isEmpty()) {
                    jsonGenerator.writeFieldName(key);
                }
                jsonGenerator.writeNumber(value);
            } catch (IOException ex) {
                throw new RuntimeException("could not serialize value", ex);
            }
        }
    }

    @Override
    public void writeDoubleValue(@Nullable String key, @Nullable Double value) {
        if (value != null) {
            try {
                if (key != null && !key.isEmpty()) {
                    jsonGenerator.writeFieldName(key);
                }
                jsonGenerator.writeNumber(value);
            } catch (IOException ex) {
                throw new RuntimeException("could not serialize value", ex);
            }
        }
    }

    @Override
    public void writeLongValue(@Nullable String key, @Nullable Long value) {
        if (value != null) {
            try {
                if (key != null && !key.isEmpty()) {
                    jsonGenerator.writeFieldName(key);
                }
                jsonGenerator.writeNumber(value);
            } catch (IOException ex) {
                throw new RuntimeException("could not serialize value", ex);
            }
        }
    }

    @Override
    public void writeUUIDValue(@Nullable String key, @Nullable UUID value) {
        if (value != null) {
            try {
                if (key != null && !key.isEmpty()) {
                    jsonGenerator.writeFieldName(key);
                }
                jsonGenerator.writeString(value.toString());
            } catch (IOException ex) {
                throw new RuntimeException("could not serialize value", ex);
            }
        }
    }

    @Override
    public void writeOffsetDateTimeValue(@Nullable String key, @Nullable OffsetDateTime value) {
        if (value != null) {
            try {
                if (key != null && !key.isEmpty()) {
                    jsonGenerator.writeFieldName(key);
                }
                jsonGenerator.writeString(value.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
            } catch (IOException ex) {
                throw new RuntimeException("could not serialize value", ex);
            }
        }
    }

    @Override
    public void writeLocalDateValue(@Nullable String key, @Nullable LocalDate value) {
        if (value != null) {
            try {
                if (key != null && !key.isEmpty()) {
                    jsonGenerator.writeFieldName(key);
                }
                jsonGenerator.writeString(value.format(DateTimeFormatter.ISO_LOCAL_DATE));

            } catch (IOException ex) {
                throw new RuntimeException("could not serialize value", ex);
            }
        }
    }

    @Override
    public void writeLocalTimeValue(@Nullable String key, @Nullable LocalTime value) {
        if (value != null) {
            try {
                if (key != null && !key.isEmpty()) {
                    jsonGenerator.writeFieldName(key);
                }
                jsonGenerator.writeString(value.format(DateTimeFormatter.ISO_LOCAL_TIME));
            } catch (IOException ex) {
                throw new RuntimeException("could not serialize value", ex);
            }
        }
    }

    @Override
    public void writePeriodValue(@Nullable String key, @Nullable Period value) {
        if (value != null) {
            try {
                if (key != null && !key.isEmpty()) {
                    jsonGenerator.writeFieldName(key);
                }
                jsonGenerator.writeString(value.toString());
            } catch (IOException ex) {
                throw new RuntimeException("could not serialize value", ex);
            }
        }
    }

    @Override
    public <T> void writeCollectionOfPrimitiveValues(@Nullable String key, @Nullable Iterable<T> values) {
        try {
            if (values != null) { //empty array is meaningful
                if (key != null && !key.isEmpty()) {
                    jsonGenerator.writeFieldName(key);
                }
                jsonGenerator.writeStartArray();
                for (final T t : values) {
                    this.writeAnyValue(null, t);
                }
                jsonGenerator.writeEndArray();
            }
        } catch (IOException ex) {
            throw new RuntimeException("could not serialize value", ex);
        }
    }

    @Override
    public <T extends Parsable> void writeCollectionOfObjectValues(@Nullable String key, @Nullable Iterable<T> values) {
        try {
            if (values != null) { //empty array is meaningful
                if (key != null && !key.isEmpty()) {
                    jsonGenerator.writeFieldName(key);
                }
                jsonGenerator.writeStartArray();
                for (final T t : values) {
                    this.writeObjectValue(null, t);
                }
                jsonGenerator.writeEndArray();
            }
        } catch (IOException ex) {
            throw new RuntimeException("could not serialize value", ex);
        }
    }

    @Override
    public <T extends Enum<T>> void writeCollectionOfEnumValues(@Nullable String key, @Nullable Iterable<T> values) {
        try {
            if (values != null) { //empty array is meaningful
                if (key != null && !key.isEmpty()) {
                    jsonGenerator.writeFieldName(key);
                }
                jsonGenerator.writeStartArray();
                for (final T t : values) {
                    this.writeEnumValue(null, t);
                }
                jsonGenerator.writeEndArray();
            }
        } catch (IOException ex) {
            throw new RuntimeException("could not serialize value", ex);
        }
    }

    @Override
    public <T extends Parsable> void writeObjectValue(@Nullable String key, @Nullable T value, @Nonnull Parsable... additionalValuesToMerge) {
        Objects.requireNonNull(additionalValuesToMerge);
        try {
            final List<Parsable> nonNullAdditionalValuesToMerge = Stream.of(additionalValuesToMerge).filter(Objects::nonNull).collect(Collectors.toList());
            if (value != null || nonNullAdditionalValuesToMerge.size() > 0) {
                if (key != null && !key.isEmpty()) {
                    jsonGenerator.writeFieldName(key);
                }
                if (onBeforeObjectSerialization != null && value != null) {
                    onBeforeObjectSerialization.accept(value);
                }
                jsonGenerator.writeStartObject();
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
                jsonGenerator.writeEndObject();
                if (onAfterObjectSerialization != null && value != null) {
                    onAfterObjectSerialization.accept(value);
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("could not serialize value", ex);
        }
    }

    @Nonnull
    @Override
    public InputStream getSerializedContent() {
        try {
            this.jsonGenerator.flush();
            return new ByteArrayInputStream(this.stream.toByteArray());
            //This copies the whole array in memory could result in memory pressure for large objects, we might want to replace by some kind of piping in the future
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public <T extends Enum<T>> void writeEnumSetValue(@Nullable String key, @Nullable EnumSet<T> values) {
        if (values != null && !values.isEmpty()) {
            final Optional<String> concatenatedValue = values.stream().map(this::getStringValueFromValuedEnum).reduce((x, y) -> {
                return x + "," + y;
            });
            concatenatedValue.ifPresent(s -> this.writeStringValue(key, s));
        }
    }

    @Override
    public <T extends Enum<T>> void writeEnumValue(@Nullable String key, @Nullable T value) {
        if (value != null) {
            this.writeStringValue(key, getStringValueFromValuedEnum(value));
        }
    }

    private <T extends Enum<T>> String getStringValueFromValuedEnum(final T value) {
        if (value instanceof ValuedEnum) {
            final ValuedEnum valued = (ValuedEnum) value;
            return valued.getValue();
        } else return null;
    }

    @Override
    public void writeNullValue(@Nullable String key) {
        try {
            if (key != null && !key.isEmpty()) {
                jsonGenerator.writeFieldName(key);
            }
            jsonGenerator.writeNull();
        } catch (IOException ex) {
            throw new RuntimeException("could not serialize value", ex);
        }
    }

    @Override
    public void writeAdditionalData(@Nonnull Map<String, Object> value) {
        for (final Map.Entry<String, Object> dataValue : value.entrySet()) {
            this.writeAnyValue(dataValue.getKey(), dataValue.getValue());
        }
    }

    private void writeNonParsableObject(@Nullable final String key, @Nullable final Object value) {
        try {
            if (key != null && !key.isEmpty())
                this.jsonGenerator.writeFieldName(key);
            if (value == null)
                this.jsonGenerator.writeNull();
            else {
                final Class<?> valueClass = value.getClass();
                for (final Field oProp : valueClass.getFields())
                    this.writeAnyValue(oProp.getName(), oProp.get(value));
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
            if (valueClass.equals(String.class))
                this.writeStringValue(key, (String) value);
            else if (valueClass.equals(Boolean.class))
                this.writeBooleanValue(key, (Boolean) value);
            else if (valueClass.equals(Byte.class))
                this.writeByteValue(key, (Byte) value);
            else if (valueClass.equals(Short.class))
                this.writeShortValue(key, (Short) value);
            else if (valueClass.equals(BigDecimal.class))
                this.writeBigDecimalValue(key, (BigDecimal) value);
            else if (valueClass.equals(Float.class))
                this.writeFloatValue(key, (Float) value);
            else if (valueClass.equals(Long.class))
                this.writeLongValue(key, (Long) value);
            else if (valueClass.equals(Integer.class))
                this.writeIntegerValue(key, (Integer) value);
            else if (valueClass.equals(UUID.class))
                this.writeUUIDValue(key, (UUID) value);
            else if (valueClass.equals(OffsetDateTime.class))
                this.writeOffsetDateTimeValue(key, (OffsetDateTime) value);
            else if (valueClass.equals(LocalDate.class))
                this.writeLocalDateValue(key, (LocalDate) value);
            else if (valueClass.equals(LocalTime.class))
                this.writeLocalTimeValue(key, (LocalTime) value);
            else if (valueClass.equals(Period.class))
                this.writePeriodValue(key, (Period) value);
            else if (value instanceof Iterable<?>)
                this.writeCollectionOfPrimitiveValues(key, (Iterable<?>) value);
            else if (!valueClass.isPrimitive())
                this.writeNonParsableObject(key, value);
            else
                throw new RuntimeException("unknown type to serialize " + valueClass.getName());
        }
    }

    @Nullable
    @Override
    public Consumer<Parsable> getOnBeforeObjectSerialization() {
        return this.onBeforeObjectSerialization;
    }

    @Nullable
    @Override
    public Consumer<Parsable> getOnAfterObjectSerialization() {
        return this.onAfterObjectSerialization;
    }

    @Nullable
    @Override
    public BiConsumer<Parsable, SerializationWriter> getOnStartObjectSerialization() {
        return this.onStartObjectSerialization;
    }

    @Override
    public void setOnBeforeObjectSerialization(@Nullable Consumer<Parsable> value) {
        this.onBeforeObjectSerialization = value;
    }

    @Override
    public void setOnAfterObjectSerialization(@Nullable Consumer<Parsable> value) {
        this.onAfterObjectSerialization = value;
    }

    @Override
    public void setOnStartObjectSerialization(@Nullable BiConsumer<Parsable, SerializationWriter> value) {
        this.onStartObjectSerialization = value;
    }

    @Override
    public void writeByteArrayValue(@Nullable String key, @Nonnull byte[] value) {
        this.writeStringValue(key, Base64.getEncoder().encodeToString(value));
    }

    @Override
    public void close() throws IOException {
        this.jsonGenerator.close();
        this.stream.close();
    }
}
