package com.rht_labs;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class ProjectTest {

    @Test
    public void testOpenEndpoint() {
        given()
          .when().get("/project/open")
          .then()
             .statusCode(200)
             .body("hello", is("world"));
    }

    @Test
    public void testSecureEndpoint() {
        given()
                .when().get("/project/secure")
                .then()
                .statusCode(204);
        // since we are not actually logged in as a user,
        // we expect no content (the endpoint normally returns
        // username), but the request to a secure endpoint
        // should still succeed with 204 due to the test
        // application.properties file disabling auth during testing.
    }

}