package com.rht_labs;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class ProjectTest {

    @Test
    public void testHelloEndpoint() {
        given()
          .when().get("/project")
          .then()
             .statusCode(200)
             .body("hello", is("world"));
    }

}