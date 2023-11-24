package io.kiota.http.jdk;

import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.util.Map;
import java.util.Set;

public class HttpRequestCompatibility {
    private HttpRequestCompatibility() {}

    public static HttpRequest convert(com.microsoft.kiota.RequestInformation requestInfo) {
        final HttpRequest.BodyPublisher body =
                requestInfo.content == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofInputStream(() -> requestInfo.content);
        final HttpRequest.Builder requestBuilder;
        try {
            requestBuilder =
                    HttpRequest.newBuilder()
                            .uri(requestInfo.getUri())
                            .method(requestInfo.httpMethod.toString(), body);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        if (requestInfo.headers != null) {
            for (final Map.Entry<String, Set<String>> headerEntry :
                    requestInfo.headers.entrySet()) {
                for (final String headerValue : headerEntry.getValue()) {
                    requestBuilder.setHeader(headerEntry.getKey(), headerValue);
                }
            }
        }

        return requestBuilder.build();
    }
}
