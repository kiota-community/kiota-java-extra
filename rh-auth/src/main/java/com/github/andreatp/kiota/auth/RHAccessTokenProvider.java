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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class RHAccessTokenProvider implements AccessTokenProvider {
    // https://access.redhat.com/articles/3626371

    public final static String RH_SSO_URL = "https://sso.redhat.com/auth/realms/redhat-external/protocol/openid-connect/token";
    public final static String RH_SSO_CLIENT_ID = "rhsm-api";

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String url;
    private final String clientId;
    private final String[] allowedHosts;
    private final String offline_token;
    private final long refreshBeforeMillis = 1000;

    private AtomicReference<String> lastRefreshToken = new AtomicReference<>(null);
    private AtomicBoolean isRefreshing = new AtomicBoolean(false);
    private AtomicReference<CountDownLatch> refreshTokenCountDown = new AtomicReference<>(new CountDownLatch(0));

    public RHAccessTokenProvider(String offline_token) {
        Objects.requireNonNull(offline_token);
        this.offline_token = offline_token;
        this.url = RH_SSO_URL;
        this.clientId = RH_SSO_CLIENT_ID;
        this.allowedHosts = new String[] { "sso.redhat.com" };
    }

    public RHAccessTokenProvider(String offline_token, String url, String clientId, String[] allowedHosts) {
        Objects.requireNonNull(offline_token);
        this.offline_token = offline_token;
        this.url = url;
        this.clientId = clientId;
        this.allowedHosts = allowedHosts;
    }

    private boolean needsRefresh() {
        return (lastRefreshToken.get() == null ||
                JWT
                        .decode(lastRefreshToken.get())
                        .getExpiresAtAsInstant()
                        .plusMillis(refreshBeforeMillis)
                        .isBefore(Instant.now()));
    }

    private void newToken() {
        if (isRefreshing.compareAndSet(false, true)) {
            if (needsRefresh()) {
                refreshTokenCountDown.set(new CountDownLatch(1));
                var data = new FormBody.Builder()
                        .add("grant_type", "refresh_token")
                        .add("client_id", clientId)
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

                lastRefreshToken.set(token);
                refreshTokenCountDown.get().countDown();
            }
            isRefreshing.set(false);
        }
    }

    @Override
    public CompletableFuture<String> getAuthorizationToken(URI uri, @Nullable Map<String, Object> additionalAuthenticationContext) {
        return CompletableFuture.supplyAsync(() -> {
            newToken();
            try {
                refreshTokenCountDown.get().await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return lastRefreshToken.get();
        });
    }

    @Override
    public AllowedHostsValidator getAllowedHostsValidator() {
        return new AllowedHostsValidator(allowedHosts);
    }
}
