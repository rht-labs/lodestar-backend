package com.redhat.labs.lodestar.resource;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.Launch;
import com.redhat.labs.lodestar.utils.IntegrationTestHelper;
import com.redhat.labs.lodestar.utils.TokenUtils;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

@QuarkusTest
@Tag("nested")
class EngagementResourceDeleteTest extends IntegrationTestHelper {

    static String validToken = TokenUtils.generateTokenString("/JwtClaimsWriter.json");

    @BeforeEach
    void setUp() {
        Map<String, List<String>> rbac = Collections.singletonMap("Residency", Collections.singletonList("writer"));
        Mockito.when(configApiClient.getPermission()).thenReturn(rbac);
    }

    @Test
    void testDeleteEngagementNotFound() {

        Mockito.when(engagementApiClient.getEngagement("1234")).thenThrow(new WebApplicationException(404));
        Mockito.when(engagementApiClient.deleteEngagement("1234")).thenReturn(Response.status(204).build());

        // DELETE
        given()
        .when()
            .auth()
            .oauth2(validToken)
            .delete("/engagements/1234")
        .then()
            .statusCode(404);
        
    }

    @Test
    void testDeleteEngagementNotAllowedEngagementType() {

        Mockito.when(engagementApiClient.getEngagement("1234")).thenReturn(
                Engagement.builder().uuid("1234").type("DO500").launch(Launch.builder().build()).build());
        Mockito.when(engagementApiClient.deleteEngagement("1234")).thenReturn(Response.status(202).build());

        // DELETE
        given()
                .when()
                .auth()
                .oauth2(validToken)
                .delete("/engagements/1234")
                .then()
                .statusCode(403);

    }

    @Test
    void testDeleteEngagementAlreadyLaunched() {

        Mockito.when(engagementApiClient.getEngagement("1234")).thenReturn(
                Engagement.builder().uuid("1234").type("Residency").launch(Launch.builder().build()).build());
        Mockito.when(engagementApiClient.deleteEngagement("1234")).thenThrow(new WebApplicationException((400)));

        // DELETE
        given()
        .when()
            .auth()
            .oauth2(validToken)
            .delete("/engagements/1234")
        .then()
            .statusCode(400);
        
    }
    
    @Test
    void testDeleteEngagementSuccess() {

        Mockito.when(engagementApiClient.getEngagement("1234")).thenReturn(
                Engagement.builder().uuid("1234").type("Residency").build());

        // DELETE
        given()
        .when()
            .auth()
            .oauth2(validToken)
            .delete("/engagements/1234")
        .then()
            .statusCode(202);
        
    }
    
}