package io.kiota.quarkus.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import foo.bar.MyApiClient;
import foo.bar.models.Greeting;
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
        MyApiClient client = new MyApiClient(adapter);

        // Act
        Greeting result = client.quarkusKiota().get();

        // Assert
        assertEquals("Hello quarkus-kiota", result.getValue());
    }
}
