package io.kiota.http.vertx;

import io.vertx.core.http.HttpMethod;

public class HttpMethodCompatibility {
    private HttpMethodCompatibility() {}

    public static HttpMethod convert(com.microsoft.kiota.HttpMethod method) {
        switch (method) {
            case GET:
                return HttpMethod.GET;
            case POST:
                return HttpMethod.POST;
            case PATCH:
                return HttpMethod.PATCH;
            case DELETE:
                return HttpMethod.DELETE;
            case OPTIONS:
                return HttpMethod.OPTIONS;
            case CONNECT:
                return HttpMethod.CONNECT;
            case PUT:
                return HttpMethod.PUT;
            case TRACE:
                return HttpMethod.TRACE;
            case HEAD:
                return HttpMethod.HEAD;
            default:
                return null;
        }
    }
}
