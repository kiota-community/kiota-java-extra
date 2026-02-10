package io.kiota.http.jdk;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.util.Map;
import java.util.Set;

public class HttpRequestCompatibility {
    private HttpRequestCompatibility() {}

    public static HttpRequest convert(com.microsoft.kiota.RequestInformation requestInfo) {
        final HttpRequest.BodyPublisher body;
        if (requestInfo.content == null) {
            body = HttpRequest.BodyPublishers.noBody();
        } else {
            try {
                final byte[] contentBytes = requestInfo.content.readAllBytes();
                if (contentBytes.length == 0) {
                    body = HttpRequest.BodyPublishers.noBody();
                } else {
                    body = HttpRequest.BodyPublishers.ofByteArray(contentBytes);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
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
