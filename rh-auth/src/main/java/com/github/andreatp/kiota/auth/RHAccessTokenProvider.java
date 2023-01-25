package com.github.andreatp.kiota.auth;

import com.auth0.jwt.JWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.kiota.authentication.AccessTokenProvider;
import com.microsoft.kiota.authentication.AllowedHostsValidator;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RHAccessTokenProvider implements AccessTokenProvider {
    // https://access.redhat.com/articles/3626371

    public final static String RH_SSO_URL = "https://sso.redhat.com/auth/realms/redhat-external/protocol/openid-connect/token";

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String url;
    private final String[] allowedHosts;
    private final String offline_token;

    private String lastRefreshToken = null;


    RHAccessTokenProvider(String offline_token) {
        this.offline_token = offline_token;
        this.url = RH_SSO_URL;
        this.allowedHosts = new String[] { "sso.redhat.com" };
    }

    RHAccessTokenProvider(String offline_token, String url, String[] allowedHosts) {
        this.offline_token = offline_token;
        this.url = url;
        this.allowedHosts = allowedHosts;
    }

    private String newToken() {
        var data = new FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("client_id", "rhsm-api")
                .add("refresh_token", offline_token)
                .build();

        var request = new Request.Builder()
                .url(url)
                .post(data)
                .build();

        String token = null;
        try {
            var response = client.newCall(request).execute();
            token = mapper.readTree(response.body().string()).get("access_token").asText();
        } catch (IOException e) {
            throw new RuntimeException("Error issuing a new token", e);
        }

        lastRefreshToken = token;
        return token;
    }

    @Override
    public CompletableFuture<String> getAuthorizationToken(URI uri, @Nullable Map<String, Object> additionalAuthenticationContext) {
        if (lastRefreshToken == null || JWT.decode(lastRefreshToken).getExpiresAtAsInstant().plusMillis(1000).isBefore(Instant.now())) {
            newToken();
        }

        return CompletableFuture.completedFuture(lastRefreshToken);
    }

    @Override
    public AllowedHostsValidator getAllowedHostsValidator() {
        return new AllowedHostsValidator(allowedHosts);
    }
}
