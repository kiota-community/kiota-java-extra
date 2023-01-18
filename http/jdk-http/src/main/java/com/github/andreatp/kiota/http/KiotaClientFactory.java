package com.github.andreatp.kiota.http;

import javax.annotation.Nonnull;
import java.net.http.HttpClient;

/**
 * This class is used to build the HttpClient instance used by the core service.
 */
public class KiotaClientFactory {
    private KiotaClientFactory() {
    }

    /**
     * Creates an OkHttpClient Builder with the default configuration and middlewares.
     *
     * @return an OkHttpClient Builder instance.
     */
    @Nonnull
    public static HttpClient Create() {
        return HttpClient
                .newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }


}