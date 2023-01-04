package io.apicurio.kiota.http;

import com.microsoft.kiota.ApiClientBuilder;
import com.microsoft.kiota.ApiException;
import com.microsoft.kiota.RequestInformation;
import com.microsoft.kiota.RequestOption;
import com.microsoft.kiota.ResponseHandler;
import com.microsoft.kiota.ResponseHandlerOption;
import com.microsoft.kiota.authentication.AuthenticationProvider;
import com.microsoft.kiota.serialization.Parsable;
import com.microsoft.kiota.serialization.ParsableFactory;
import com.microsoft.kiota.serialization.ParseNode;
import com.microsoft.kiota.serialization.ParseNodeFactory;
import com.microsoft.kiota.serialization.ParseNodeFactoryRegistry;
import com.microsoft.kiota.serialization.SerializationWriterFactory;
import com.microsoft.kiota.serialization.SerializationWriterFactoryRegistry;
import com.microsoft.kiota.store.BackingStoreFactory;
import com.microsoft.kiota.store.BackingStoreFactorySingleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class JdkHttpRequestAdapter implements com.microsoft.kiota.RequestAdapter {
    private final static String contentTypeHeaderKey = "Content-Type";
    private String baseUrl = "";
    private ParseNodeFactory pNodeFactory;
    private SerializationWriterFactory sWriterFactory;
    private final AuthenticationProvider authProvider;
    private final HttpClient client;

    public JdkHttpRequestAdapter(@Nonnull final AuthenticationProvider authenticationProvider) {
        this(authenticationProvider, null, null, null);
    }

    @SuppressWarnings("LambdaLast")
    public JdkHttpRequestAdapter(@Nonnull final AuthenticationProvider authenticationProvider, @Nullable final ParseNodeFactory parseNodeFactory) {
        this(authenticationProvider, parseNodeFactory, null, null);
    }

    @SuppressWarnings("LambdaLast")
    public JdkHttpRequestAdapter(@Nonnull final AuthenticationProvider authenticationProvider, @Nullable final ParseNodeFactory parseNodeFactory, @Nullable final SerializationWriterFactory serializationWriterFactory) {
        this(authenticationProvider, parseNodeFactory, serializationWriterFactory, null);
    }

    @SuppressWarnings("LambdaLast")
    public JdkHttpRequestAdapter(@Nonnull final AuthenticationProvider authenticationProvider, @Nullable final ParseNodeFactory parseNodeFactory, @Nullable final SerializationWriterFactory serializationWriterFactory, @Nullable final HttpClient client) {
        this.authProvider = Objects.requireNonNull(authenticationProvider, "parameter authenticationProvider cannot be null");
        if (client == null) {
            this.client = KiotaClientFactory.Create();
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

    @Override
    public void setBaseUrl(@Nonnull String baseUrl) {
        this.baseUrl = Objects.requireNonNull(baseUrl);
    }

    @Nonnull
    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    @Nonnull
    @Override
    public SerializationWriterFactory getSerializationWriterFactory() {
        return sWriterFactory;
    }

    @Override
    public void enableBackingStore(@Nullable final BackingStoreFactory backingStoreFactory) {
        this.pNodeFactory = Objects.requireNonNull(ApiClientBuilder.enableBackingStoreForParseNodeFactory(pNodeFactory));
        this.sWriterFactory = Objects.requireNonNull(ApiClientBuilder.enableBackingStoreForSerializationWriterFactory(sWriterFactory));
        if (backingStoreFactory != null) {
            BackingStoreFactorySingleton.instance = backingStoreFactory;
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

    private void setBaseUrlForRequestInformation(@Nonnull final RequestInformation requestInfo) {
        Objects.requireNonNull(requestInfo);
        requestInfo.pathParameters.put("baseurl", getBaseUrl());
    }

    private final static String claimsKey = "claims";

    private CompletableFuture<HttpResponse<InputStream>> getHttpResponseMessage(@Nonnull final RequestInformation requestInfo, @Nullable final String claims) {
        Objects.requireNonNull(requestInfo, "parameter requestInfo cannot be null");
        this.setBaseUrlForRequestInformation(requestInfo);
        final Map<String, Object> additionalContext = new HashMap<String, Object>();
        if (claims != null && !claims.isEmpty()) {
            additionalContext.put(claimsKey, claims);
        }
        return this.authProvider.authenticateRequest(requestInfo, additionalContext)
                .thenCompose(x -> {
                    try {
                        return this.client.sendAsync(this.getRequestFromRequestInformation(requestInfo), HttpResponse.BodyHandlers.ofInputStream());
                    } catch (URISyntaxException | MalformedURLException ex) {
                        return CompletableFuture.failedFuture(ex);
                    }
                });
    }

    private HttpRequest getRequestFromRequestInformation(@Nonnull final RequestInformation requestInfo) throws URISyntaxException, MalformedURLException {
        final URL requestURL = requestInfo.getUri().toURL();
        final HttpRequest.BodyPublisher body = requestInfo.content == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofInputStream(() -> requestInfo.content);
        final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(requestURL.toURI())
                .method(requestInfo.httpMethod.toString(), body);

        if (requestInfo.headers != null) {
            for (final Map.Entry<String, Set<String>> headerEntry : requestInfo.headers.entrySet()) {
                for (final String headerValue : headerEntry.getValue()) {
                    requestBuilder.setHeader(headerEntry.getKey(), headerValue);
                }
            }
        }

        return requestBuilder.build();
    }

    private HttpResponse throwIfFailedResponse(@Nonnull final HttpResponse response, @Nullable final HashMap<String, ParsableFactory<? extends Parsable>> errorMappings) throws ApiException {
        if (response.statusCode() >= 200 && response.statusCode() < 300) return response;

        final String statusCodeAsString = Integer.toString(response.statusCode());
        final Integer statusCode = response.statusCode();
        if (errorMappings == null ||
                !errorMappings.containsKey(statusCodeAsString) &&
                        !(statusCode >= 400 && statusCode < 500 && errorMappings.containsKey("4XX")) &&
                        !(statusCode >= 500 && statusCode < 600 && errorMappings.containsKey("5XX"))) {
            final ApiException result = new ApiException("the server returned an unexpected status code and no error class is registered for this code " + statusCode);
            throw result;
        }

        final ParsableFactory<? extends Parsable> errorClass = errorMappings.containsKey(statusCodeAsString) ?
                errorMappings.get(statusCodeAsString) :
                (statusCode >= 400 && statusCode < 500 ?
                        errorMappings.get("4XX") :
                        errorMappings.get("5XX"));
        if (response.body() == null) {
            final ApiException result = new ApiException("service returned status code" + statusCode + " but no response body was found");
            throw result;
        } else {
            // TODO: this is not going to be that much helpful ...
            final ApiException result = new ApiException("service returned status code" + statusCode + " with body: " + response.body());
            throw result;
        }
    }

    private boolean shouldReturnNull(final HttpResponse response) {
        final int statusCode = response.statusCode();
        return statusCode == 204;
    }

    private ParseNode getRootParseNode(final HttpResponse<InputStream> response) throws IOException {
        final InputStream body = response.body();
        if (body == null) {
            return null;
        }
        // TODO: check that the conte type extraction is good enough
        final ParseNode rootNode = pNodeFactory.getParseNode(response.headers().firstValue(contentTypeHeaderKey).orElse(null), body);
        return rootNode;
    }

    @Nullable
    @Override
    public <ModelType> CompletableFuture<ModelType> sendPrimitiveAsync(@Nonnull RequestInformation requestInfo, @Nonnull Class<ModelType> targetClass, @Nullable HashMap<String, ParsableFactory<? extends Parsable>> errorMappings) {
        Objects.requireNonNull(requestInfo, "parameter requestInfo cannot be null");
        Objects.requireNonNull(targetClass, "parameter targetClass cannot be null");
        return this.getHttpResponseMessage(requestInfo, null)
                .thenCompose(response -> {
                    final ResponseHandler responseHandler = getResponseHandler(requestInfo);
                    if (responseHandler == null) {
                        try {
                            this.throwIfFailedResponse(response, errorMappings);
                            if (this.shouldReturnNull(response)) {
                                return CompletableFuture.completedFuture(null);
                            }
                            if (targetClass == Void.class) {
                                return CompletableFuture.completedFuture(null);
                            } else {
                                if (targetClass == InputStream.class) {
                                    final InputStream body = response.body();
                                    if (body == null) {
                                        return CompletableFuture.completedFuture(null);
                                    }
                                    return CompletableFuture.completedFuture((ModelType) body);
                                }
                                final ParseNode rootNode = getRootParseNode(response);
                                if (rootNode == null) {
                                    return CompletableFuture.completedFuture(null);
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
                                } else if (targetClass == Period.class) {
                                    result = rootNode.getPeriodValue();
                                } else if (targetClass == byte[].class) {
                                    result = rootNode.getByteArrayValue();
                                } else {
                                    throw new RuntimeException("unexpected payload type " + targetClass.getName());
                                }
                                return CompletableFuture.completedFuture((ModelType) result);
                            }
                        } catch (ApiException ex) {
                            return new CompletableFuture<ModelType>() {{
                                this.completeExceptionally(ex);
                            }};
                        } catch (IOException ex) {
                            return new CompletableFuture<ModelType>() {{
                                this.completeExceptionally(new RuntimeException("failed to read the response body", ex));
                            }};
                        }
                    } else {
                        return responseHandler.handleResponseAsync(response, errorMappings);
                    }
                });
    }

    @Nullable
    @Override
    public <ModelType> CompletableFuture<List<ModelType>> sendPrimitiveCollectionAsync(@Nonnull RequestInformation requestInfo, @Nonnull Class<ModelType> targetClass, @Nullable HashMap<String, ParsableFactory<? extends Parsable>> errorMappings) {
        Objects.requireNonNull(requestInfo, "parameter requestInfo cannot be null");

        return this.getHttpResponseMessage(requestInfo, null)
                .thenCompose(response -> {
                    final ResponseHandler responseHandler = getResponseHandler(requestInfo);
                    if (responseHandler == null) {
                        try {
                            this.throwIfFailedResponse(response, errorMappings);
                            if (this.shouldReturnNull(response)) {
                                return CompletableFuture.completedFuture(null);
                            }
                            final ParseNode rootNode = getRootParseNode(response);
                            if (rootNode == null) {
                                return CompletableFuture.completedFuture(null);
                            }
                            final List<ModelType> result = rootNode.getCollectionOfPrimitiveValues(targetClass);
                            return CompletableFuture.completedFuture(result);
                        } catch (ApiException ex) {
                            return new CompletableFuture<List<ModelType>>() {{
                                this.completeExceptionally(ex);
                            }};
                        } catch (IOException ex) {
                            return new CompletableFuture<List<ModelType>>() {{
                                this.completeExceptionally(new RuntimeException("failed to read the response body", ex));
                            }};
                        }
                    } else {
                        return responseHandler.handleResponseAsync(response, errorMappings);
                    }
                });
    }

    @Nullable
    @Override
    public <ModelType extends Parsable> CompletableFuture<List<ModelType>> sendCollectionAsync(@Nonnull RequestInformation requestInfo, @Nonnull ParsableFactory<ModelType> factory, @Nullable HashMap<String, ParsableFactory<? extends Parsable>> errorMappings) {
        Objects.requireNonNull(requestInfo, "parameter requestInfo cannot be null");
        Objects.requireNonNull(factory, "parameter factory cannot be null");

        return this.getHttpResponseMessage(requestInfo, null)
                .thenCompose(response -> {
                    final ResponseHandler responseHandler = getResponseHandler(requestInfo);
                    if (responseHandler == null) {
                        try {
                            this.throwIfFailedResponse(response, errorMappings);
                            if (this.shouldReturnNull(response)) {
                                return CompletableFuture.completedFuture(null);
                            }
                            final ParseNode rootNode = getRootParseNode(response);
                            if (rootNode == null) {
                                return CompletableFuture.completedFuture(null);
                            }
                            final List<ModelType> result = rootNode.getCollectionOfObjectValues(factory);
                            return CompletableFuture.completedFuture(result);
                        } catch (ApiException ex) {
                            return new CompletableFuture<List<ModelType>>() {{
                                this.completeExceptionally(ex);
                            }};
                        } catch (IOException ex) {
                            return new CompletableFuture<List<ModelType>>() {{
                                this.completeExceptionally(new RuntimeException("failed to read the response body", ex));
                            }};
                        }
                    } else {
                        return responseHandler.handleResponseAsync(response, errorMappings);
                    }
                });
    }

    @Nullable
    @Override
    public <ModelType extends Enum<ModelType>> CompletableFuture<ModelType> sendEnumAsync(@Nonnull RequestInformation requestInfo, @Nonnull Class<ModelType> targetClass, @Nullable HashMap<String, ParsableFactory<? extends Parsable>> errorMappings) {
        Objects.requireNonNull(requestInfo, "parameter requestInfo cannot be null");
        Objects.requireNonNull(targetClass, "parameter targetClass cannot be null");
        return this.getHttpResponseMessage(requestInfo, null)
                .thenCompose(response -> {
                    final ResponseHandler responseHandler = getResponseHandler(requestInfo);
                    if (responseHandler == null) {
                        try {
                            this.throwIfFailedResponse(response, errorMappings);
                            if (this.shouldReturnNull(response)) {
                                return CompletableFuture.completedFuture(null);
                            }
                            final ParseNode rootNode = getRootParseNode(response);
                            if (rootNode == null) {
                                return CompletableFuture.completedFuture(null);
                            }

                            final Object result = rootNode.getEnumValue(targetClass);
                            return CompletableFuture.completedFuture((ModelType) result);
                        } catch (ApiException ex) {
                            return new CompletableFuture<ModelType>() {{
                                this.completeExceptionally(ex);
                            }};
                        } catch (IOException ex) {
                            return new CompletableFuture<ModelType>() {{
                                this.completeExceptionally(new RuntimeException("failed to read the response body", ex));
                            }};
                        }
                    } else {
                        return responseHandler.handleResponseAsync(response, errorMappings);
                    }
                });
    }

    @Nullable
    @Override
    public <ModelType extends Enum<ModelType>> CompletableFuture<List<ModelType>> sendEnumCollectionAsync(@Nonnull RequestInformation requestInfo, @Nonnull Class<ModelType> targetClass, @Nullable HashMap<String, ParsableFactory<? extends Parsable>> errorMappings) {
        Objects.requireNonNull(requestInfo, "parameter requestInfo cannot be null");
        Objects.requireNonNull(targetClass, "parameter targetClass cannot be null");
        return this.getHttpResponseMessage(requestInfo, null)
                .thenCompose(response -> {
                    final ResponseHandler responseHandler = getResponseHandler(requestInfo);
                    if (responseHandler == null) {
                        try {
                            this.throwIfFailedResponse(response, errorMappings);
                            if (this.shouldReturnNull(response)) {
                                return CompletableFuture.completedFuture(null);
                            }
                            final ParseNode rootNode = getRootParseNode(response);
                            if (rootNode == null) {
                                return CompletableFuture.completedFuture(null);
                            }
                            final Object result = rootNode.getCollectionOfEnumValues(targetClass);
                            return CompletableFuture.completedFuture((List<ModelType>) result);
                        } catch (ApiException ex) {
                            return new CompletableFuture<List<ModelType>>() {{
                                this.completeExceptionally(ex);
                            }};
                        } catch (IOException ex) {
                            return new CompletableFuture<List<ModelType>>() {{
                                this.completeExceptionally(new RuntimeException("failed to read the response body", ex));
                            }};
                        }
                    } else {
                        return responseHandler.handleResponseAsync(response, errorMappings);
                    }
                });
    }

    @Nullable
    @Override
    public <ModelType extends Parsable> CompletableFuture<ModelType> sendAsync(@Nonnull RequestInformation requestInfo, @Nonnull ParsableFactory<ModelType> factory, @Nullable HashMap<String, ParsableFactory<? extends Parsable>> errorMappings) {
        Objects.requireNonNull(requestInfo, "parameter requestInfo cannot be null");
        Objects.requireNonNull(factory, "parameter factory cannot be null");

        return this.getHttpResponseMessage(requestInfo, null)
                .thenCompose(response -> {
                    final ResponseHandler responseHandler = getResponseHandler(requestInfo);
                    if (responseHandler == null) {
                        try {
                            this.throwIfFailedResponse(response, errorMappings);
                            if (this.shouldReturnNull(response)) {
                                return CompletableFuture.completedFuture(null);
                            }
                            final ParseNode rootNode = getRootParseNode(response);
                            if (rootNode == null) {
                                return CompletableFuture.completedFuture(null);
                            }
                            final ModelType result = rootNode.getObjectValue(factory);
                            return CompletableFuture.completedFuture(result);
                        } catch (ApiException ex) {
                            return new CompletableFuture<ModelType>() {{
                                this.completeExceptionally(ex);
                            }};
                        } catch (IOException ex) {
                            return new CompletableFuture<ModelType>() {{
                                this.completeExceptionally(new RuntimeException("failed to read the response body", ex));
                            }};
                        }
                    } else {
                        return responseHandler.handleResponseAsync(response, errorMappings);
                    }
                });
    }

}
