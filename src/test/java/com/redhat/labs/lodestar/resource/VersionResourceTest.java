package com.redhat.labs.lodestar.resource;

import com.redhat.labs.lodestar.model.status.VersionManifest;
import com.redhat.labs.lodestar.rest.client.StatusApiClient;
import com.redhat.labs.lodestar.utils.ResourceLoader;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.PropertyNamingStrategy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@Tag("nested")
class VersionResourceTest {

    JsonbConfig config = new JsonbConfig().withFormatting(true)
            .withPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CASE_WITH_UNDERSCORES);
    Jsonb quarkusJsonb = JsonbBuilder.create(config);

    @InjectMock
    @RestClient
    StatusApiClient statusApiClient;

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