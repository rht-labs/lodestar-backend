package com.redhat.labs.omp.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.util.HashMap;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import com.redhat.labs.utils.EmbeddedMongoTest;
import com.redhat.labs.utils.TokenUtils;

import io.quarkus.test.junit.QuarkusTest;

@EmbeddedMongoTest
@QuarkusTest
public class ConfigResourceTest {

    @ConfigProperty(name = "configFileCacheKey", defaultValue = "schema/config.yml")
    String configFileCacheKey;

    /*
     * 
     * SCENARIOS:
     * 
     * SUCCESS -
     *   token supplied, has correct role, in cache
     * 
     * FAIL - 
     *   token has wrong role - 403
     *   TODO: The following need to be tested, but no hook to specify return from mock git api service bean
     *   config not in cache, call git api and config not returned - fail?
     *   config not in cache, call git api and get error response
     *   config not in cache, call git api and get network error
     * 
     */

    @Test
    public void testGetConfigTokenHasWrongRole() throws Exception {

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
    public void testGetConfigInGitRepo() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsReader.json", timeClaims);

        given()
            .when()
                .auth()
                    .oauth2(token)
                .get("/config")
            .then()
                .statusCode(200)
                .body(is("{ \"content\": \"content\", \"encoding\": \"base64\", \"file_path\": \"myfile.yaml\" }"))
                .body("content", is("content"))
                .body("file_path", is("myfile.yaml"));
        
    }

    @Test
    public void testGetConfigInGitRepoV2() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsReader.json", timeClaims);

        given()
            .headers("Accept-version", "v2")
            .when()
                .auth()
                    .oauth2(token)
                .get("/config")
            .then()
                .statusCode(200)
                .body(is("{ \"hello\" : \"world\" }"));

    }

}
