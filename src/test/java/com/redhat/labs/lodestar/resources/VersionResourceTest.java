package com.redhat.labs.lodestar.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;

import javax.inject.Inject;
import javax.json.bind.Jsonb;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.redhat.labs.lodestar.model.Version;
import com.redhat.labs.lodestar.model.status.VersionManifestV1;
import com.redhat.labs.lodestar.repository.ActiveSyncRepository;
import com.redhat.labs.lodestar.repository.EngagementRepository;
import com.redhat.labs.lodestar.rest.client.LodeStarGitLabAPIService;
import com.redhat.labs.lodestar.rest.client.LodeStarStatusApiClient;
import com.redhat.labs.lodestar.utils.ResourceLoader;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;

@QuarkusTest
@Tag("integration")
class VersionResourceTest {

    @Inject
    Jsonb jsonb;

    @InjectMock
    ActiveSyncRepository acRepository;

    @InjectMock
    EngagementRepository eRepository;

    @InjectMock
    @RestClient
    LodeStarStatusApiClient statusClient;
    
    @InjectMock
    @RestClient
    LodeStarGitLabAPIService gitApiClient;

    @Test
    void testValidResourceVersion() {

        Version v = Version.builder().gitCommit("abcdef").gitTag("v1.1").build();
        Mockito.when(gitApiClient.getVersion()).thenReturn(v);

        given()
        .when()
            .contentType(ContentType.JSON)
            .get("/api/v1/version")
        .then()
            .statusCode(200)
            .body("containers.version",hasItem("master-abcdef"))
            .body("containers.version", hasItem("v1.1"));
    }   

    @Test
    void testValidResourceVersion1() {

        Version v = Version.builder().gitCommit("abcdef").gitTag("v1.1").build();
        Mockito.when(gitApiClient.getVersion()).thenReturn(v);
        
        given()
        .when()
            .contentType(ContentType.JSON)
            .header("Accept-version", "v1")
            .get("/api/version")
        .then()
            .statusCode(200)
            .statusCode(200)
            .body("containers.version",hasItem("master-abcdef"))
            .body("containers.version", hasItem("v1.1"));

    }

    @Test
    void testValidResourceVersion2() {

        given()
        .when()
            .contentType(ContentType.JSON)
            .header("Accept-version", "v2")
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
    void testValidResourceVersionInvalidAcceptVersion() {

        given()
        .when()
            .contentType(ContentType.JSON)
            .header("Accept-version", "v8")
            .get("/api/version")
        .then()
            .statusCode(400);

    }

    @Test
    void testValidResourceVersionManifest() {

        String json = ResourceLoader.load("status-service/version-manifest.yaml");
        VersionManifestV1 vm = jsonb.fromJson(json, VersionManifestV1.class);
        Mockito.when(statusClient.getVersionManifestV1()).thenReturn(vm);

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