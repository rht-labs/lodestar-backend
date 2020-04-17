package com.redhat.labs.omp.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class VersionResourceTest {

    @Test
    public void testValidResourceVersion() {
        given()
        .when()
            .get("/api/v1/version")
        .then()
            .statusCode(200)
            .body(is("{\"versions\":[{\"application\":\"omp-git-api-container\",\"git_commit\":\"git-commit\",\"git_tag\":\"git-tag\",\"version\":\"git-commit\"},{\"application\":\"omp-backend-container\","
            		+ "\"git_commit\":\"abcdef\",\"git_tag\":\"77.8\"}]}"));
    }   
}
    