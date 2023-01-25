
import com.auth0.jwt.JWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.kiota.authentication.AccessTokenProvider;
import com.microsoft.kiota.authentication.AllowedHostsValidator;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RHAccessTokenProvider implements AccessTokenProvider {
    // https://access.redhat.com/articles/3626371

    final OkHttpClient client = new OkHttpClient();
    final ObjectMapper mapper = new ObjectMapper();
    final String url = "https://sso.redhat.com/auth/realms/redhat-external/protocol/openid-connect/token";
    final String offline_token;

    String lastRefreshToken = null;

    RHAccessTokenProvider(String offline_token) {
        this.offline_token = offline_token;
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

    @NotNull
    @Override
    public CompletableFuture<String> getAuthorizationToken(@NotNull URI uri, @Nullable Map<String, Object> additionalAuthenticationContext) {
        if (lastRefreshToken == null || JWT.decode(lastRefreshToken).getExpiresAtAsInstant().plusMillis(1000).isBefore(Instant.now())) {
            newToken();
        }

        return CompletableFuture.completedFuture(lastRefreshToken);
    }

    @NotNull
    @Override
    public AllowedHostsValidator getAllowedHostsValidator() {
        return new AllowedHostsValidator("sso.redhat.com");
    }
}
