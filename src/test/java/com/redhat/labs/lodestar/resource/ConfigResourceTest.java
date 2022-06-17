package com.redhat.labs.lodestar.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.labs.lodestar.rest.client.ConfigApiClient;
import com.redhat.labs.lodestar.utils.ResourceLoader;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.mockito.InjectMock;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.redhat.labs.lodestar.utils.TokenUtils;

import io.quarkus.test.junit.QuarkusTest;

import java.util.Map;

@QuarkusTest
@TestHTTPEndpoint(ConfigResource.class)
@Tag("nested")
class ConfigResourceTest {

    static String VALID_TOKEN;

    @InjectMock
    @RestClient
    public ConfigApiClient configApiClient;

    ObjectMapper om = new ObjectMapper();

    @BeforeAll
    static void generateToken() {
        VALID_TOKEN = TokenUtils.generateTokenString("/JwtClaimsReader.json");
    }

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

    @Test
    void testRegionOptions() throws Exception {
        String regions = ResourceLoader.load("config-region-options.json");
        Map<String, String> regionMap = om.readValue(regions, Map.class);
        Mockito.when(configApiClient.getRegionOptions()).thenReturn(regionMap);
        given().when().auth().oauth2(VALID_TOKEN).get("/region/options").then().statusCode(200)
                .body("$", Matchers.hasKey("region1"))
                .body("$", Matchers.hasKey("region2"));
    }

    @Test
    void testArtifactOptions() throws Exception {
        String artifacts = ResourceLoader.load("config-artifact-options.json");
        Map<String, String> artifactsMap = om.readValue(artifacts, Map.class);
        Mockito.when(configApiClient.getArtifactOptions()).thenReturn(artifactsMap);
        given().when().auth().oauth2(VALID_TOKEN).get("/artifact/options").then().statusCode(200)
                .body("$", Matchers.hasKey("doc"))
                .body("$", Matchers.hasKey("paper"));
    }

    @Test
    void testEngagementOptions() throws Exception {
        String engagements = ResourceLoader.load("config-engagement-options.json");
        Map<String, String> engagementsMap = om.readValue(engagements, Map.class);
        Mockito.when(configApiClient.getEngagementOptions()).thenReturn(engagementsMap);
        given().when().auth().oauth2(VALID_TOKEN).get("/engagement/options").then().statusCode(200)
                .body("$", Matchers.hasKey("res"))
                .body("$", Matchers.hasKey("training"));
    }

    @Test
    void testParticipantOptions() throws Exception {
        String participants = ResourceLoader.load("config-participant-options.json");
        Map<String, String> pariticipantsMap = om.readValue(participants, Map.class);
        Mockito.when(configApiClient.getParticipantOptions()).thenReturn(pariticipantsMap);
        given().when().auth().oauth2(VALID_TOKEN).get("/participant/options").then().statusCode(200)
                .body("$", Matchers.hasKey("arole"))
                .body("$", Matchers.hasKey("brole"));

        Mockito.when(configApiClient.getParticipantOptions("DO")).thenReturn(pariticipantsMap);
        given().queryParam("engagementType", "DO").when().auth().oauth2(VALID_TOKEN).get("/participant/options").then().statusCode(200)
                .body("$", Matchers.hasKey("arole"))
                .body("$", Matchers.hasKey("brole"));
    }

}