package com.redhat.labs.lodestar.resource;

import com.redhat.labs.lodestar.rest.client.*;
import com.redhat.labs.lodestar.utils.TokenUtils;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@Tag("nested")
class RefreshResourceTest {

    @InjectMock
    @RestClient
    public ActivityApiClient activityClient;

    @InjectMock
    @RestClient
    public ArtifactApiClient artifactClient;

    @InjectMock
    @RestClient
    public ParticipantApiClient participantClient;

    @InjectMock
    @RestClient
    public HostingEnvironmentApiClient hostingEnvironmentApiClient;

    @InjectMock
    @RestClient
    public EngagementStatusApiClient engagementStatusApiClient;

    @InjectMock
    @RestClient
    public EngagementApiClient engagementApiClient;
    
    static String validToken;

    @BeforeAll
    static void setUp() {
        validToken = TokenUtils.generateTokenString("/JwtClaimsWriter.json");
    }

    @Test
    void testEngagementReload() {
        given().queryParam("engagements", true).when().auth().oauth2(validToken)
                .put("/engagements/refresh").then().statusCode(202);

        Mockito.verify(engagementApiClient, Mockito.timeout(1000)).refresh(Collections.emptySet());
    }

    @Test
    void testActivityReload() {

        given().when().auth().oauth2(validToken)
                .put("/engagements/refresh").then().statusCode(400);
        
        given().queryParam("activity", true).when().auth().oauth2(validToken)
                .put("/engagements/refresh").then().statusCode(202);
        
        Mockito.verify(activityClient, Mockito.timeout(1000)).refresh();
    }

    @Test
    void testHostingReload() {

        given().queryParam("hosting", true).when().auth().oauth2(validToken)
                .put("/engagements/refresh").then().statusCode(202).body("size()", equalTo(1)).body("[0]", equalTo("hosting"));

        Mockito.verify(hostingEnvironmentApiClient, Mockito.timeout(1000)).refresh();
    }
    
    @Test
    void testParticipantReload() {

        given().queryParam("participants", true).when().auth().oauth2(validToken)
        .put("/engagements/refresh").then().statusCode(202);
        
        Mockito.verify(participantClient, Mockito.timeout(1000)).refreshParticipants();
    }

    @Test
    void testStatusReload() {

        given().queryParam("status", true).when().auth().oauth2(validToken)
                .put("/engagements/refresh").then().statusCode(202);

        Mockito.verify(engagementStatusApiClient, Mockito.timeout(1000)).refresh();
    }
    
    @Test
    void testParticipantReloadFail() {
        
        Mockito.when(participantClient.refreshParticipants()).thenReturn(Response.serverError().build());

        given().queryParam("participants", true).when().auth().oauth2(validToken)
        .put("/engagements/refresh").then().statusCode(202);
        
        Mockito.verify(participantClient, Mockito.timeout(1000)).refreshParticipants();
    }
    
    @Test
    void testArtifactsReload() {

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
    void testDbRefreshWithoutPurge() {

        given()
            .auth()
            .oauth2(validToken)
            .queryParam("purgeFirst", false)
            .queryParam("engagements", true)
        .when()
            .put("/engagements/refresh")
        .then()
            .statusCode(202);

    }

    @Test
    void testRefreshState() {
        Mockito.when(engagementApiClient.refreshStates()).thenReturn(Response.ok().build());
        given()
            .auth()
            .oauth2(validToken)
        .when()
            .put("/engagements/refresh/states")
        .then()
            .statusCode(200);
    }
}
