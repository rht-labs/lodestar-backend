package com.redhat.labs.lodestar.resource;

import static io.restassured.RestAssured.given;

import java.util.HashMap;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.redhat.labs.lodestar.utils.IntegrationTestHelper;
import com.redhat.labs.lodestar.utils.TokenUtils;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@Tag("nested")
class RefreshResourceTest extends IntegrationTestHelper {
    
    static String validToken;

    @BeforeAll
    static void setUp() throws Exception {
        HashMap<String, Long> timeClaims = new HashMap<>();
        validToken = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);
    }

    @Test
    void testActivityReload() throws Exception {

        given().when().auth().oauth2(validToken)
                .put("/engagements/refresh").then().statusCode(400);
        
        given().queryParam("activity", true).when().auth().oauth2(validToken)
        .put("/engagements/refresh").then().statusCode(202);
        
        Mockito.verify(activityClient, Mockito.timeout(1000)).refresh();
    }
    
    @Test
    void testParticipantReload() throws Exception {

        given().queryParam("participants", true).when().auth().oauth2(validToken)
        .put("/engagements/refresh").then().statusCode(202);
        
        Mockito.verify(participantClient, Mockito.timeout(1000)).refreshParticipants();
    }
    
    @Test
    void testParticipantReloadFail() throws Exception {
        
        Mockito.when(participantClient.refreshParticipants()).thenReturn(Response.serverError().build());

        given().queryParam("participants", true).when().auth().oauth2(validToken)
        .put("/engagements/refresh").then().statusCode(202);
        
        Mockito.verify(participantClient, Mockito.timeout(1000)).refreshParticipants();
    }
    
    @Test
    void testArtifactsReload() throws Exception {

        given().queryParam("artifacts", true).when().auth().oauth2(validToken)
        .put("/engagements/refresh").then().statusCode(202);
        
        Mockito.verify(artifactClient, Mockito.timeout(1000)).refreshArtifacts();
    }
    
    @Test
    void testDbRefreshWithPurge() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        given()
            .auth()
            .oauth2(token)
            .queryParam("engagements", true)
            .queryParam("purgeFirst", true)
        .when()
            .put("/engagements/refresh")
        .then()
            .statusCode(202);

    }

    @Test
    void testDbRefreshWithoutPurge() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        given()
            .auth()
            .oauth2(token)
            .queryParam("purgeFirst", false)
            .queryParam("engagements", true)
        .when()
            .put("/engagements/refresh")
        .then()
            .statusCode(202);

    }
}
