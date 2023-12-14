package io.kiota.quarkus.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.apisdk.example.yaml.ApiClient;
import io.apisdk.example.yaml.models.Greeting;
import io.kiota.http.vertx.VertXRequestAdapter;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.Vertx;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class QuarkusKiotaResourceTest {
    @Inject Vertx vertx;

    @Test
    public void testHelloEndpoint() {
        given().when()
                .get("/quarkus-kiota")
                .then()
                .statusCode(200)
                .body(is("{\"value\":\"Hello quarkus-kiota\"}"));
    }

    @Test
    public void testHelloEndpointUsingTheKiotaClient() throws Exception {
        // Arrange
        var adapter = new VertXRequestAdapter(vertx);
        adapter.setBaseUrl("http://localhost:8081");
        ApiClient client = new ApiClient(adapter);

        // Act
        Greeting result = client.quarkusKiota().get();

        // Assert
        assertEquals("Hello quarkus-kiota", result.getValue());
    }
}
