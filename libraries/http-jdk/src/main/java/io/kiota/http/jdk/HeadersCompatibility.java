package io.kiota.http.jdk;

import com.microsoft.kiota.RequestHeaders;
import com.microsoft.kiota.ResponseHeaders;
import jakarta.annotation.Nonnull;
import java.net.http.HttpHeaders;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Compatibility class to bridge OkHttp Headers and Kiota Headers
 */
public class HeadersCompatibility {
    private HeadersCompatibility() {}

    @Nonnull
    public static ResponseHeaders getResponseHeaders(@Nonnull final HttpHeaders headers) {
        Objects.requireNonNull(headers);
        final ResponseHeaders responseHeaders = new ResponseHeaders();
        headers.map()
                .forEach(
                        (name, value) -> {
                            Objects.requireNonNull(name);
                            responseHeaders.put(name, new HashSet<>(value));
                        });
        return responseHeaders;
    }

    @Nonnull
    public static HttpHeaders getHttpHeaders(@Nonnull final RequestHeaders headers) {
        Map<String, List<String>> map =
                headers.entrySet().stream()
                        .map((elem) -> Map.entry(elem.getKey(), new ArrayList<>(elem.getValue())))
                        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        return HttpHeaders.of(map, (x, y) -> true);
    }
}
