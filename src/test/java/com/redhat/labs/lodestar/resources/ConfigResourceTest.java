package com.redhat.labs.lodestar.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.util.HashMap;

import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.redhat.labs.lodestar.utils.TokenUtils;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@Tag("integration")
class ConfigResourceTest extends EngagementResourceTestHelper {

    @ConfigProperty(name = "configFileCacheKey", defaultValue = "schema/config.yml")
    String configFileCacheKey;

    @Test
    void testGetConfigTokenHasWrongRole() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsUnknown.json", timeClaims);

        given()
            .when()
                .auth()
                    .oauth2(token)
                .get("/config")
            .then()
                .statusCode(403);
        
    }

    @Test
    void testGetConfigInGitRepo() throws Exception {

        String body = "{ \"content\": \"content\", \"encoding\": \"base64\", \"file_path\": \"myfile.yaml\" }";

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsReader.json", timeClaims);

        Mockito.when(gitApiClient.getConfigFile()).thenReturn(Response.ok(body).build());

        given()
            .when()
                .auth()
                    .oauth2(token)
                .get("/config")
            .then()
                .statusCode(200)
                .body(is(body))
                .body("content", is("content"))
                .body("file_path", is("myfile.yaml"));
        
    }

    @Test
    void testGetConfigInGitRepoV2() throws Exception {

        String body = "{ \"hello\" : \"world\" }";

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsReader.json", timeClaims);

        Mockito.when(gitApiClient.getConfigFileV2()).thenReturn(Response.ok(body).build());

        given()
            .headers("Accept-version", "v2")
            .when()
                .auth()
                    .oauth2(token)
                .get("/config")
            .then()
                .statusCode(200)
                .body(is(body));

    }

}