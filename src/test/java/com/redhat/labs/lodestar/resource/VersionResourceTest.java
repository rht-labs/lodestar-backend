package com.redhat.labs.lodestar.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.redhat.labs.lodestar.model.status.VersionManifest;
import com.redhat.labs.lodestar.utils.IntegrationTestHelper;
import com.redhat.labs.lodestar.utils.ResourceLoader;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@Tag("nested")
class VersionResourceTest extends IntegrationTestHelper {

    @Test
    void testValidResourceVersion() {

        given()
        .when()
            .contentType(ContentType.JSON)
            .get("/api/version")
        .then()
            .statusCode(200)
            .body("git_commit", is("abcdef"))
            .body("git_tag", is("master"));

    }

    @Test
    void testValidResourceVersionNoAcceptVersionSupplied() {

        given()
        .when()
            .contentType(ContentType.JSON)
            .get("/api/version")
        .then()
            .statusCode(200)
            .body("git_commit", is("abcdef"))
            .body("git_tag", is("master"));

    }

    @Test
    void testValidResourceVersionManifest() {

        String json = ResourceLoader.load("status-service/version-manifest.yaml");
        VersionManifest vm = quarkusJsonb.fromJson(json, VersionManifest.class);
        Mockito.when(statusApiClient.getVersionManifest()).thenReturn(vm);

        given()
        .when()
            .contentType(ContentType.JSON)
            .get("/api/version/manifest")
        .then()
            .statusCode(200)
            .body("main_version.name",is("ball"))
            .body("main_version.value", is("v2.0"))
            .body("component_versions.name", hasItem("launcher"))
            .body("component_versions.value", hasItem("v1.1"));

    }

}