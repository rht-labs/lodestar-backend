package com.redhat.labs.omp.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class InfoResourceTest {
    @Test
    public void testOpenEndpoint() {
        given()
          .when().get("/info/open")
          .then()
             .statusCode(200)
             .body("hello", is("world"));
    }

    @Test
    public void testSecureEndpoint() {
        given()
                .when().get("/info/secure")
                .then()
                .statusCode(204);
        // since we are not actually logged in as a user,
        // we expect no content (the endpoint normally returns
        // username), but the request to a secure endpoint
        // should still succeed with 204 due to the test
        // application.properties file disabling auth during testing.
    }
}
