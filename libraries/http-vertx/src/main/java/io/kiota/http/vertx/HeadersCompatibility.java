package io.kiota.http.vertx;

import com.microsoft.kiota.RequestHeaders;
import com.microsoft.kiota.ResponseHeaders;
import io.vertx.core.MultiMap;
import jakarta.annotation.Nonnull;
import java.util.HashSet;
import java.util.Objects;

/**
 * Compatibility class to bridge OkHttp Headers and Kiota Headers
 */
public class HeadersCompatibility {
    private HeadersCompatibility() {}

    @Nonnull
    public static ResponseHeaders getResponseHeaders(@Nonnull final MultiMap headers) {
        Objects.requireNonNull(headers);
        final ResponseHeaders responseHeaders = new ResponseHeaders();
        headers.names()
                .forEach(
                        (name) -> {
                            Objects.requireNonNull(name);
                            responseHeaders.put(name, new HashSet<>(headers.getAll(name)));
                        });
        return responseHeaders;
    }

    @Nonnull
    public static MultiMap getMultiMap(@Nonnull final RequestHeaders headers) {
        MultiMap result = MultiMap.caseInsensitiveMultiMap();
        headers.entrySet().forEach((elem) -> result.add(elem.getKey(), elem.getValue()));
        return result;
    }
}
