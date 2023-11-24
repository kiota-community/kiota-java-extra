package io.kiota.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.apisdk.example.yaml.ApiClient;
import io.apisdk.example.yaml.models.Greeting;
import io.kiota.http.vertx.VertXRequestAdapter;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.auth.oauth2.Oauth2Credentials;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.OAuth2WebClient;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import jakarta.inject.Inject;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(OidcWiremockTestResource.class)
public class QuarkusKiotaResourceTest {

    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String keycloakUrl;

    @Inject Vertx vertx;

    static final String CLIENT_ID = UUID.randomUUID().toString();
    static final String CLIENT_SECRET = UUID.randomUUID().toString();

    @Test
    public void testHelloEndpointUsingTheKiotaClient() throws Exception {
        OAuth2Options options =
                new OAuth2Options()
                        .setFlow(OAuth2FlowType.CLIENT)
                        .setClientId(CLIENT_ID)
                        .setTokenPath(keycloakUrl + "token")
                        .setClientSecret(CLIENT_SECRET);

        // Adding using random UUIDs as the mock does not care about concrete values
        OAuth2Auth oAuth2Auth = OAuth2Auth.create(vertx, options);

        Oauth2Credentials oauth2Credentials = new Oauth2Credentials();

        OAuth2WebClient oAuth2WebClient =
                OAuth2WebClient.create(WebClient.create(vertx), oAuth2Auth)
                        .withCredentials(oauth2Credentials);

        // Arrange
        var adapter = new VertXRequestAdapter(oAuth2WebClient);
        adapter.setBaseUrl("http://localhost:8081");
        ApiClient client = new ApiClient(adapter);

        // Act
        Greeting result = client.quarkusKiota().get();

        // Assert
        assertEquals("Hello quarkus-kiota", result.getValue());
    }

    @Test
    public void testVertxExample() throws Exception {
        // Arrange
        Vertx vertx = Vertx.vertx();
        WebClient webClient =
                WebClient.create(
                        vertx); // Don't do this in a regular application, the vertx instance must
        // be injected

        OAuth2Options options =
                new OAuth2Options()
                        .setFlow(OAuth2FlowType.CLIENT)
                        .setClientId(UUID.randomUUID().toString())
                        .setTokenPath(keycloakUrl + "token")
                        .setClientSecret(UUID.randomUUID().toString());

        // Adding random as the mock does not care about concrete values
        OAuth2Auth oAuth2Auth = OAuth2Auth.create(vertx, options);

        Oauth2Credentials oauth2Credentials = new Oauth2Credentials();

        OAuth2WebClient oAuth2WebClient =
                OAuth2WebClient.create(webClient, oAuth2Auth).withCredentials(oauth2Credentials);

        HttpRequest<Greeting> greetingHttpRequest =
                oAuth2WebClient
                        .get(8081, "localhost", "/quarkus-kiota")
                        .as(BodyCodec.json(Greeting.class));

        CompletableFuture<Greeting> greetingCompletableFuture = new CompletableFuture<>();

        greetingHttpRequest
                .send()
                .onSuccess(
                        successResult -> {
                            greetingCompletableFuture.complete(successResult.body());
                        });

        assertEquals("Hello quarkus-kiota", greetingCompletableFuture.get().getValue());
    }
}
