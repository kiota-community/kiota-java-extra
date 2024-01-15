package io.kiota.quarkus.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class QuarkusKiotaResourceTest {

    @Test
    public void testHelloEndpoint() {
        given().when()
                .get("/quarkus-kiota?name=me")
                .then()
                .statusCode(200)
                .body(is("{\"value\":\"Hello quarkus-kiota me\"}"));
    }

    @Test
    public void testHelloEndpointUsingTheKiotaClient() {
        given().when()
                .get("/quarkus-kiota2")
                .then()
                .statusCode(200)
                .body(is("{\"value\":\"Hello quarkus-kiota myself\"}"));
    }
}
