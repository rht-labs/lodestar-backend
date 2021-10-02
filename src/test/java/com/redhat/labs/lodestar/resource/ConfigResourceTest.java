package com.redhat.labs.lodestar.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import javax.ws.rs.core.Response;

import com.redhat.labs.lodestar.rest.client.ConfigApiClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.mockito.InjectMock;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.redhat.labs.lodestar.utils.TokenUtils;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(ConfigResource.class)
@Tag("nested")
class ConfigResourceTest {

    @InjectMock
    @RestClient
    public ConfigApiClient configApiClient;

    @Test
    void testGetConfigTokenHasWrongRole()  {

        String token = TokenUtils.generateTokenString("/JwtClaimsUnknown.json");

        given()
            .when()
                .auth()
                    .oauth2(token)
                .get()
            .then()
                .statusCode(403);
        
    }

    @Test
    void testGetConfigWithoutType() {

        String body = "{ \"content\": \"content\", \"encoding\": \"base64\", \"file_path\": \"myfile.yaml\" }";

        String token = TokenUtils.generateTokenString("/JwtClaimsReader.json");

        Mockito.when(configApiClient.getRuntimeConfig(null)).thenReturn(Response.ok(body).build());

        given()
            .when()
                .auth()
                    .oauth2(token)
                .get()
            .then()
                .statusCode(200)
                .body(is(body))
                .body("content", is("content"))
                .body("file_path", is("myfile.yaml"));
        
    }

    @Test
    void testGetConfigWithType() {

        String body = "{ \"hello\" : \"world\" }";

        String token = TokenUtils.generateTokenString("/JwtClaimsReader.json");

        Mockito.when(configApiClient.getRuntimeConfig("one")).thenReturn(Response.ok(body).build());

        given()
            .queryParam("type", "one")
            .when()
                .auth()
                    .oauth2(token)
                .get()
            .then()
                .statusCode(200)
                .body(is(body));

    }

    @Test
    void testVoidCache() {
        String token = TokenUtils.generateTokenString("/JwtClaimsReader.json");

        given().when().auth().oauth2(token).put("/rbac/cache").then().statusCode(200);
    }

}