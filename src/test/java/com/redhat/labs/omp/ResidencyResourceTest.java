package com.redhat.labs.omp;

import static io.restassured.RestAssured.given;

import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ResidencyResourceTest {

	@Test
    public void saveResidency() {
        given()
          .contentType(MediaType.APPLICATION_JSON)
        .when().post("/residency")
        .then()
          .statusCode(200);
    }
}
