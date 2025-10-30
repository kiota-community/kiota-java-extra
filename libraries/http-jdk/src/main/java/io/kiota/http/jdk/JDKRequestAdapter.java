package io.kiota.http.jdk;

import com.microsoft.kiota.ApiClientBuilder;
import com.microsoft.kiota.ApiException;
import com.microsoft.kiota.ApiExceptionBuilder;
import com.microsoft.kiota.PeriodAndDuration;
import com.microsoft.kiota.RequestAdapter;
import com.microsoft.kiota.RequestInformation;
import com.microsoft.kiota.RequestOption;
import com.microsoft.kiota.ResponseHandler;
import com.microsoft.kiota.ResponseHandlerOption;
import com.microsoft.kiota.ResponseHeaders;
import com.microsoft.kiota.serialization.Parsable;
import com.microsoft.kiota.serialization.ParsableFactory;
import com.microsoft.kiota.serialization.ParseNode;
import com.microsoft.kiota.serialization.ParseNodeFactory;
import com.microsoft.kiota.serialization.ParseNodeFactoryRegistry;
import com.microsoft.kiota.serialization.SerializationWriterFactory;
import com.microsoft.kiota.serialization.SerializationWriterFactoryRegistry;
import com.microsoft.kiota.serialization.ValuedEnumParser;
import com.microsoft.kiota.store.BackingStoreFactory;
import com.microsoft.kiota.store.BackingStoreFactorySingleton;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** RequestAdapter implementation for VertX */
public class JDKRequestAdapter implements RequestAdapter {
    private static final String contentTypeHeaderKey = "Content-Type";
    @Nonnull private final HttpClient client;
    @Nonnull private ParseNodeFactory pNodeFactory;
    @Nonnull private SerializationWriterFactory sWriterFactory;
    @Nonnull private String baseUrl = "";

    public void setBaseUrl(@Nonnull final String baseUrl) {
        this.baseUrl = Objects.requireNonNull(baseUrl);
    }

    @Nonnull
    public String getBaseUrl() {
        return baseUrl;
    }

    public JDKRequestAdapter() {
        this(null, null, null);
    }

    public JDKRequestAdapter(@Nonnull final HttpClient client) {
        this(client, null, null);
    }

    public JDKRequestAdapter(
            @Nullable final HttpClient client, @Nullable final ParseNodeFactory parseNodeFactory) {
        this(client, parseNodeFactory, null);
    }

    public JDKRequestAdapter(
            @Nullable final HttpClient client,
            @Nullable final ParseNodeFactory parseNodeFactory,
            @Nullable final SerializationWriterFactory serializationWriterFactory) {
        if (client == null) {
            this.client = HttpClient.newHttpClient();
        } else {
            this.client = client;
        }
        if (parseNodeFactory == null) {
            pNodeFactory = ParseNodeFactoryRegistry.defaultInstance;
        } else {
            pNodeFactory = parseNodeFactory;
        }

        if (serializationWriterFactory == null) {
            sWriterFactory = SerializationWriterFactoryRegistry.defaultInstance;
        } else {
            sWriterFactory = serializationWriterFactory;
        }
    }

    @Nonnull
    public SerializationWriterFactory getSerializationWriterFactory() {
        return sWriterFactory;
    }

    public void enableBackingStore(@Nullable final BackingStoreFactory backingStoreFactory) {
        this.pNodeFactory =
                Objects.requireNonNull(
                        ApiClientBuilder.enableBackingStoreForParseNodeFactory(pNodeFactory));
        this.sWriterFactory =
                Objects.requireNonNull(
                        ApiClientBuilder.enableBackingStoreForSerializationWriterFactory(
                                sWriterFactory));
        if (backingStoreFactory != null) {
            BackingStoreFactorySingleton.instance = backingStoreFactory;
        }
    }

    private static final String nullRequestInfoParameter = "parameter requestInfo cannot be null";
    private static final String nullEnumParserParameter = "parameter enumParser cannot be null";
    private static final String nullFactoryParameter = "parameter factory cannot be null";

    @Nullable
    public <ModelType extends Parsable> List<ModelType> sendCollection(
            @Nonnull final RequestInformation requestInfo,
            @Nullable final HashMap<String, ParsableFactory<? extends Parsable>> errorMappings,
            @Nonnull final ParsableFactory<ModelType> factory) {
        Objects.requireNonNull(requestInfo, nullRequestInfoParameter);
        Objects.requireNonNull(factory, nullFactoryParameter);

        HttpResponse response = this.getHttpResponseMessage(requestInfo);
        final ResponseHandler responseHandler = getResponseHandler(requestInfo);
        if (responseHandler == null) {
            this.throwIfFailedResponse(response, errorMappings);
            if (this.shouldReturnNull(response)) {
                return null;
            }
            final ParseNode rootNode = getRootParseNode(response);
            if (rootNode == null) {
                return null;
            }
            final List<ModelType> result = rootNode.getCollectionOfObjectValues(factory);
            return result;
        } else {
            return responseHandler.handleResponse(response, errorMappings);
        }
    }

    private ResponseHandler getResponseHandler(final RequestInformation requestInfo) {
        final Collection<RequestOption> requestOptions = requestInfo.getRequestOptions();
        for (final RequestOption rOption : requestOptions) {
            if (rOption instanceof ResponseHandlerOption) {
                final ResponseHandlerOption option = (ResponseHandlerOption) rOption;
                return option.getResponseHandler();
            }
        }
        return null;
    }

    @Nullable
    public <ModelType extends Parsable> ModelType send(
            @Nonnull final RequestInformation requestInfo,
            @Nullable final HashMap<String, ParsableFactory<? extends Parsable>> errorMappings,
            @Nonnull final ParsableFactory<ModelType> factory) {
        Objects.requireNonNull(requestInfo, nullRequestInfoParameter);
        Objects.requireNonNull(factory, nullFactoryParameter);

        HttpResponse response = this.getHttpResponseMessage(requestInfo);
        final ResponseHandler responseHandler = getResponseHandler(requestInfo);
        if (responseHandler == null) {
            this.throwIfFailedResponse(response, errorMappings);
            if (this.shouldReturnNull(response)) {
                return null;
            }
            final ParseNode rootNode = getRootParseNode(response);
            if (rootNode == null) {
                return null;
            }
            final ModelType result = rootNode.getObjectValue(factory);
            return result;
        } else {
            return responseHandler.handleResponse(response, errorMappings);
        }
    }

    @Nullable
    public <ModelType> ModelType sendPrimitive(
            @Nonnull final RequestInformation requestInfo,
            @Nullable final HashMap<String, ParsableFactory<? extends Parsable>> errorMappings,
            @Nonnull final Class<ModelType> targetClass) {
        Objects.requireNonNull(requestInfo, nullRequestInfoParameter);
        Objects.requireNonNull(targetClass, "parameter targetClass cannot be null");

        HttpResponse response = this.getHttpResponseMessage(requestInfo);
        final ResponseHandler responseHandler = getResponseHandler(requestInfo);
        if (responseHandler == null) {
            this.throwIfFailedResponse(response, errorMappings);
            if (this.shouldReturnNull(response)) {
                return null;
            }
            if (targetClass == Void.class) {
                return null;
            } else {
                if (targetClass == InputStream.class) {
                    // TODO: verify streaming responses
                    return (ModelType) response.body();
                }
                final ParseNode rootNode = getRootParseNode(response);
                if (rootNode == null) {
                    return null;
                }
                Object result;
                if (targetClass == Boolean.class) {
                    result = rootNode.getBooleanValue();
                } else if (targetClass == Byte.class) {
                    result = rootNode.getByteValue();
                } else if (targetClass == String.class) {
                    result = rootNode.getStringValue();
                } else if (targetClass == Short.class) {
                    result = rootNode.getShortValue();
                } else if (targetClass == BigDecimal.class) {
                    result = rootNode.getBigDecimalValue();
                } else if (targetClass == Double.class) {
                    result = rootNode.getDoubleValue();
                } else if (targetClass == Integer.class) {
                    result = rootNode.getIntegerValue();
                } else if (targetClass == Float.class) {
                    result = rootNode.getFloatValue();
                } else if (targetClass == Long.class) {
                    result = rootNode.getLongValue();
                } else if (targetClass == UUID.class) {
                    result = rootNode.getUUIDValue();
                } else if (targetClass == OffsetDateTime.class) {
                    result = rootNode.getOffsetDateTimeValue();
                } else if (targetClass == LocalDate.class) {
                    result = rootNode.getLocalDateValue();
                } else if (targetClass == LocalTime.class) {
                    result = rootNode.getLocalTimeValue();
                } else if (targetClass == PeriodAndDuration.class) {
                    result = rootNode.getPeriodAndDurationValue();
                } else if (targetClass == byte[].class) {
                    result = rootNode.getByteArrayValue();
                } else {
                    throw new RuntimeException("unexpected payload type " + targetClass.getName());
                }
                return (ModelType) result;
            }
        } else {
            return responseHandler.handleResponse(response, errorMappings);
        }
    }

    @Nullable
    public <ModelType extends Enum<ModelType>> ModelType sendEnum(
            @Nonnull final RequestInformation requestInfo,
            @Nullable final HashMap<String, ParsableFactory<? extends Parsable>> errorMappings,
            @Nonnull final ValuedEnumParser<ModelType> enumParser) {
        Objects.requireNonNull(requestInfo, nullRequestInfoParameter);
        Objects.requireNonNull(enumParser, nullEnumParserParameter);

        HttpResponse response = this.getHttpResponseMessage(requestInfo);
        final ResponseHandler responseHandler = getResponseHandler(requestInfo);
        if (responseHandler == null) {
            this.throwIfFailedResponse(response, errorMappings);
            if (this.shouldReturnNull(response)) {
                return null;
            }
            final ParseNode rootNode = getRootParseNode(response);
            if (rootNode == null) {
                return null;
            }
            final Object result = rootNode.getEnumValue(enumParser);
            return (ModelType) result;
        } else {
            return responseHandler.handleResponse(response, errorMappings);
        }
    }

    @Nullable
    public <ModelType extends Enum<ModelType>> List<ModelType> sendEnumCollection(
            @Nonnull final RequestInformation requestInfo,
            @Nullable final HashMap<String, ParsableFactory<? extends Parsable>> errorMappings,
            @Nonnull final ValuedEnumParser<ModelType> enumParser) {
        Objects.requireNonNull(requestInfo, nullRequestInfoParameter);
        Objects.requireNonNull(enumParser, nullEnumParserParameter);

        HttpResponse response = this.getHttpResponseMessage(requestInfo);
        final ResponseHandler responseHandler = getResponseHandler(requestInfo);
        if (responseHandler == null) {
            this.throwIfFailedResponse(response, errorMappings);
            if (this.shouldReturnNull(response)) {
                return null;
            }
            final ParseNode rootNode = getRootParseNode(response);
            if (rootNode == null) {
                return null;
            }
            final Object result = rootNode.getCollectionOfEnumValues(enumParser);
            return (List<ModelType>) result;
        } else {
            return responseHandler.handleResponse(response, errorMappings);
        }
    }

    @Nullable
    public <ModelType> List<ModelType> sendPrimitiveCollection(
            @Nonnull final RequestInformation requestInfo,
            @Nullable final HashMap<String, ParsableFactory<? extends Parsable>> errorMappings,
            @Nonnull final Class<ModelType> targetClass) {
        Objects.requireNonNull(requestInfo, nullRequestInfoParameter);

        HttpResponse response = getHttpResponseMessage(requestInfo);
        final ResponseHandler responseHandler = getResponseHandler(requestInfo);
        if (responseHandler == null) {
            this.throwIfFailedResponse(response, errorMappings);
            if (this.shouldReturnNull(response)) {
                return null;
            }
            final ParseNode rootNode = getRootParseNode(response);
            if (rootNode == null) {
                return null;
            }
            final List<ModelType> result = rootNode.getCollectionOfPrimitiveValues(targetClass);
            return result;
        } else {
            return responseHandler.handleResponse(response, errorMappings);
        }
    }

    @Nullable
    private ParseNode getRootParseNode(final HttpResponse<InputStream> response) {
        final InputStream rawInputStream = response.body();
        if (rawInputStream == null) {
            return null;
        }

        final String contentType = response.headers().firstValue(contentTypeHeaderKey).orElse(null);
        if (contentType == null) {
            return null;
        }
        return pNodeFactory.getParseNode(contentType, rawInputStream);
    }

    private boolean shouldReturnNull(final HttpResponse response) {
        return response.statusCode() == 204;
    }

    private HttpResponse<InputStream> throwIfFailedResponse(
            @Nonnull final HttpResponse response,
            @Nullable final HashMap<String, ParsableFactory<? extends Parsable>> errorMappings) {
        if (response.statusCode() >= 200 && response.statusCode() < 300) return response;

        final String statusCodeAsString = Integer.toString(response.statusCode());
        final int statusCode = response.statusCode();
        final ResponseHeaders responseHeaders =
                HeadersCompatibility.getResponseHeaders(response.headers());
        if (errorMappings == null
                || !errorMappings.containsKey(statusCodeAsString)
                        && !(statusCode >= 400
                                && statusCode < 500
                                && errorMappings.containsKey("4XX"))
                        && !(statusCode >= 500
                                && statusCode < 600
                                && errorMappings.containsKey("5XX"))) {
            final ApiException result =
                    new ApiExceptionBuilder()
                            .withMessage(
                                    "the server returned an unexpected status code and no error"
                                            + " class is registered for this code "
                                            + statusCode)
                            .withResponseStatusCode(statusCode)
                            .withResponseHeaders(responseHeaders)
                            .build();
            throw result;
        }

        final ParsableFactory<? extends Parsable> errorClass =
                errorMappings.containsKey(statusCodeAsString)
                        ? errorMappings.get(statusCodeAsString)
                        : (statusCode >= 400 && statusCode < 500
                                ? errorMappings.get("4XX")
                                : errorMappings.get("5XX"));
        final ParseNode rootNode = getRootParseNode(response);
        if (rootNode == null) {
            final ApiException result =
                    new ApiExceptionBuilder()
                            .withMessage(
                                    "service returned status code"
                                            + statusCode
                                            + " but no response body was found")
                            .withResponseStatusCode(statusCode)
                            .withResponseHeaders(responseHeaders)
                            .build();
            throw result;
        }
        ApiException result =
                new ApiExceptionBuilder(() -> rootNode.getObjectValue(errorClass))
                        .withResponseStatusCode(statusCode)
                        .withResponseHeaders(responseHeaders)
                        .build();
        throw result;
    }

    private HttpResponse<InputStream> getHttpResponseMessage(
            @Nonnull final RequestInformation requestInfo) {
        Objects.requireNonNull(requestInfo, nullRequestInfoParameter);
        this.setBaseUrlForRequestInformation(requestInfo);
        try {
            return this.client.send(
                    convertToNativeRequest(requestInfo), HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void setBaseUrlForRequestInformation(@Nonnull final RequestInformation requestInfo) {
        Objects.requireNonNull(requestInfo);
        requestInfo.pathParameters.put("baseurl", getBaseUrl());
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    public HttpRequest convertToNativeRequest(@Nonnull final RequestInformation requestInfo) {
        Objects.requireNonNull(requestInfo, nullRequestInfoParameter);
        return getRequestFromRequestInformation(requestInfo);
    }

    protected @Nonnull HttpRequest getRequestFromRequestInformation(
            @Nonnull final RequestInformation requestInfo) {
        return HttpRequestCompatibility.convert(requestInfo);
    }
}
