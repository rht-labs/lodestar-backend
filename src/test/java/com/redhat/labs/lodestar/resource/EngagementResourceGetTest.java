package com.redhat.labs.lodestar.resource;

import com.redhat.labs.lodestar.model.*;
import com.redhat.labs.lodestar.model.filter.ArtifactOptions;
import com.redhat.labs.lodestar.rest.client.*;
import com.redhat.labs.lodestar.utils.IntegrationTestHelper;
import com.redhat.labs.lodestar.utils.TokenUtils;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@Tag("nested")
class EngagementResourceGetTest extends IntegrationTestHelper {

    static String validToken =  TokenUtils.generateTokenString("/JwtClaimsWriter.json");
    static String defaultSort = "lastUpdate|desc";

    @InjectMock
    @RestClient
    HostingEnvironmentApiClient hostingEnvironmentApiClient;

    @InjectMock
    @RestClient
    ArtifactApiClient artifactApiClient;

    @InjectMock
    @RestClient
    ParticipantApiClient participantApiClient;

    @InjectMock
    @RestClient
    CategoryApiClient categoryApiClient;

    @InjectMock
    @RestClient
    ActivityApiClient activityApiClient;

    @BeforeEach
    void setUp() {
        Mockito.when(artifactApiClient.getArtifacts(Mockito.any(ArtifactOptions.class)))
                .thenReturn(javax.ws.rs.core.Response.ok(Collections.emptyList()).build());
    }

    @Test
    void testGetEngagementWithAuthAndRoleSuccess() {

        Engagement engagement = Engagement.builder().uuid("1234").type("Residency").customerName("Customer").projectName("Project").build();

        Mockito.when(engagementApiClient.getEngagement("1234")).thenReturn(engagement);

        // GET
        given()
            .when()
                .auth()
                .oauth2(validToken)
                .get("/engagements/1234")
            .then()
                .statusCode(200)
                .body("customer_name", equalTo(engagement.getCustomerName()))
                .body("project_name", equalTo(engagement.getProjectName()))
                .body("uuid", equalTo("1234"));

    }

    @Test
    void testGetEngagementDoesNotExist() {

        Mockito.when(engagementApiClient.getEngagement("1234")).thenThrow(new WebApplicationException(404));

        // GET
        given()
            .when()
                .auth()
                .oauth2(validToken)
                .get("/engagements/1234")
            .then()
                .statusCode(404);

    }

    @Test
    void testGetEngagementsSuccessNoEngagements() {

        Mockito.when(engagementApiClient.getEngagements(0, 1000, Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), null, defaultSort))
                .thenReturn(javax.ws.rs.core.Response.ok(Collections.emptyList()).build());

        // GET engagement
        given()
            .when()
                .auth()
                .oauth2(validToken)
                .contentType(ContentType.JSON)
                .get("/engagements")
            .then()
                .statusCode(200)
                .body("size()", equalTo(0));

        Mockito.verify(engagementApiClient).getEngagements(0, 1000, Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), null, defaultSort);
    }

    @Test
    void testGetEngagementsSuccess() {

        Engagement engagement1 = Engagement.builder().uuid("1234").type("Residency").customerName("Customer").projectName("Project1")
                .artifactCount(4).build();
        Engagement engagement2 = Engagement.builder().uuid("1235").type("Residency").customerName("Customer").projectName("Project2")
                .participantCount(10).build();

        Mockito.when(engagementApiClient.getEngagements(0, 1000, Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), null, defaultSort))
                .thenReturn(Response.ok(List.of(engagement1, engagement2)).build());

        // GET engagement
        given()
            .when()
                .auth()
                .oauth2(validToken)
                .contentType(ContentType.JSON)
                .get("/engagements")
            .then()
                .statusCode(200)
                .body("size()", equalTo(2))
                .body("[0].participant_count", equalTo(0))
                .body("[1].participant_count", equalTo(10))
                .body("[0].artifact_count", equalTo(4))
                .body("[1].artifact_count", equalTo(0));

        Mockito.verify(engagementApiClient).getEngagements(0, 1000, Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), null, defaultSort);
    }

    // TODO Abandoning support for include / exclude in v2.
    // Initial impl will instead not retrieve subcomponents when doing a get engagement list
    // It will only retrieve a full engagement by uuid
    // uuid will default to retrieving everything
    // sub-components are: engagement status, activity, hosting env, participants, artifacts
    @Test
    void testGetAllWithExcludeAndInclude() {

        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json");
        Mockito.when(engagementApiClient.getEngagements(0, 1000, Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), null, defaultSort))
                .thenReturn(Response.ok(Collections.emptyList()).build());

        // get all
        given()
            .auth()
            .oauth2(token)
            .queryParam("include", "somevalue")
            .queryParam("exclude", "anothervalue")
            .contentType(ContentType.JSON)
        .when()
            .get("/engagements")
        .then().statusCode(200);

    }

    @Test
    void testGetAllWithInclude() {

        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json");
        Mockito.when(engagementApiClient.getEngagements(0, 1000, Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), null, defaultSort))
                .thenReturn(Response.ok(Collections.emptyList()).build());

        // get all
        given()
            .auth()
            .oauth2(token)
            .queryParam("include", "somevalue")
            .contentType(ContentType.JSON)
        .when()
            .get("/engagements")
        .then()
            .statusCode(200)
            .body("size()", equalTo(0));
    }
    
    @Test
    void testGetArtifactTypes() {
        Mockito.when(artifactApiClient.getTypes(Mockito.anyList())).thenReturn(Collections.singleton("a1a beachfront avenue"));

        given()
            .auth()
            .oauth2(validToken)
            .contentType(ContentType.JSON)
            .queryParam("suggest", "a")
        .when()
            .get("/engagements/artifact/types")
        .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0]", equalTo("a1a beachfront avenue"));

    }

    @Test
    void testGetArtifactsV2() {

        EngagementArtifact art = EngagementArtifact.builder().uuid("demo-1").title("demo 1").type("demo").engagementUuid("euid").linkAddress("http://demo-1").build();
        Mockito.when(artifactClient.getArtifacts(Mockito.any(ArtifactOptions.class))).thenReturn(javax.ws.rs.core.Response.ok(List.of(art))
                .header("x-total-artifacts", 1).build());

        Mockito.when(engagementApiClient.getEngagement("euid")).thenReturn(Engagement.builder().customerName("Customer")
                .projectName("Project").build());

        given()
            .auth()
            .oauth2(validToken)
            .contentType(ContentType.JSON)
            .header("Accept-version", "v2")
        .when()
            .get("/engagements/artifacts")
        .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].customer_name", equalTo("Customer"))
            .body("[0].title", equalTo("demo 1"))
            .body("[0].link_address", equalTo("http://demo-1"));


    }

    @Test
    void testGetScores() {

        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json");

        given()
            .auth()
            .oauth2(token)
            .contentType(ContentType.JSON)
        .when()
            .get("/engagements/scores")
        .then()
            .statusCode(501);

    }

    @Test
    void testGetEngagementUserSummary() {

        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json");

        Map<String, Long> enabled = new HashMap<>();
        enabled.put("Red Hat", 1000000000L);
        enabled.put("Others", 6L);
        enabled.put("All", 1000000006L);

        Mockito.when(participantApiClient.getEnabledParticipants(Collections.emptyList()))
                        .thenReturn(enabled);

        // GET
        given()
            .when()
                .auth()
                .oauth2(token)
                .get("/engagements/users/summary")
            .then()
                .statusCode(200).body("all_users_count", equalTo(1000000006))
                .body("other_users_count", equalTo(6))
                .body("rh_users_count", equalTo(1000000000));

    }

    @Test
    void testGetEngagementUserSummaryRegion() {

        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json");

        Map<String, Long> enabled = new HashMap<>();
        enabled.put("Red Hat", 1000000000L);
        enabled.put("Others", 6L);
        enabled.put("All", 1000000006L);

        Mockito.when(participantApiClient.getEnabledParticipants(List.of("na")))
                .thenReturn(enabled);

        // GET
        given()
                .queryParam("search", "engagement_region=na")
        .when()
                .auth()
                .oauth2(token)
                .get("/engagements/users/summary")
            .then()
                .statusCode(200).body("all_users_count", equalTo(1000000006))
                .body("other_users_count", equalTo(6))
                .body("rh_users_count", equalTo(1000000000));

        enabled = new HashMap<>();
        enabled.put("Red Hat", 7L);
        enabled.put("Others", 5L);
        enabled.put("All", 12L);

        Mockito.when(participantApiClient.getEnabledParticipants(Collections.emptyList()))
                .thenReturn(enabled);

        given()
                .queryParam("search", "not_engagement_region=na")
                .when()
                .auth()
                .oauth2(token)
                .get("/engagements/users/summary")
                .then()
                .statusCode(200).body("all_users_count", equalTo(12))
                .body("other_users_count", equalTo(5))
                .body("rh_users_count", equalTo(7));

    }
}