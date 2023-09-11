package com.redhat.cloud.kiota.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.kiota.authentication.AccessTokenProvider;
import com.microsoft.kiota.authentication.AllowedHostsValidator;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import jakarta.annotation.Nullable;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class RHAccessTokenProvider implements AccessTokenProvider {
    // https://access.redhat.com/articles/3626371

    public final static String SSO_TOKEN_PATH = "/protocol/openid-connect/token";
    public final static String RH_SSO_URL = "https://sso.redhat.com/auth/realms/redhat-external/protocol/openid-connect/token";
    public final static String RH_SSO_CLIENT_ID = "cloud-services"; // or "rhsm-api"

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String url;
    private final String clientId;
    private final String[] allowedHosts;
    private final String offline_token;
    private final long refreshBeforeMillis = (System.getenv("RH_ACCESS_TOKEN_REFRESH_BEFORE") == null) ? 60000 : Long.parseLong(System.getenv("RH_ACCESS_TOKEN_REFRESH_BEFORE"));

    private AtomicReference<String> lastRefreshToken = new AtomicReference<>(null);
    private AtomicBoolean isRefreshing = new AtomicBoolean(false);
    private AtomicReference<CountDownLatch> refreshTokenCountDown = new AtomicReference<>(new CountDownLatch(0));

    public RHAccessTokenProvider(String offline_token) {
        Objects.requireNonNull(offline_token);
        this.offline_token = offline_token;
        String url = RH_SSO_URL;
        String clientId = RH_SSO_CLIENT_ID;
        String[] allowedHosts = new String[] { "sso.redhat.com" };
        try {
            DecodedJWT decoded = JWT.decode(offline_token);
            String issuer = decoded.getIssuer();
            String allowedHost = URI.create(issuer).getHost();
            url = issuer + SSO_TOKEN_PATH;
            allowedHosts = new String[] { allowedHost };

            String payload = new String(Base64.getDecoder().decode(decoded.getPayload()), StandardCharsets.UTF_8);
            clientId = mapper.readTree(payload).get("azp").textValue();
        } catch (Exception e) {
            // ignore: fallback to static defaults
        }

        this.url = url;
        this.allowedHosts = allowedHosts;
        this.clientId = clientId;
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
                        .minusMillis(refreshBeforeMillis)
                        .isBefore(Instant.now()));
    }

    private void newToken() {
        if (isRefreshing.compareAndSet(false, true)) {
            try {
                if (needsRefresh()) {
                    refreshTokenCountDown.set(new CountDownLatch(1));
                    FormBody data = new FormBody.Builder()
                            .add("grant_type", "refresh_token")
                            .add("client_id", clientId)
                            .add("refresh_token", offline_token)
                            .build();

                    Request request = new Request.Builder()
                            .url(url)
                            .post(data)
                            .build();

                    String token = null;
                    try {
                        Response response = client.newCall(request).execute();
                        int code = response.code();
                        if (code == 200) {
                            String body = response.body().string();
                            try {
                                token = mapper.readTree(body).get("access_token").asText();
                            } catch (Exception e) {
                                throw new RuntimeException("Error issuing a new token, received answer with body " + body, e);
                            }
                        } else {
                            throw new RuntimeException("Error issuing a new token, received answer code " + code);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Error issuing a new token", e);
                    }

                    lastRefreshToken.set(token);
                    refreshTokenCountDown.get().countDown();
                }
            } finally {
                isRefreshing.set(false);
            }
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
