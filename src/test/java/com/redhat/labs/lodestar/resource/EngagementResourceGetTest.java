package com.redhat.labs.lodestar.resource;

import com.google.common.collect.Lists;
import com.redhat.labs.lodestar.model.*;
import com.redhat.labs.lodestar.model.filter.ArtifactOptions;
import com.redhat.labs.lodestar.model.pagination.PagedEngagementResults;
import com.redhat.labs.lodestar.rest.client.*;
import com.redhat.labs.lodestar.utils.IntegrationTestHelper;
import com.redhat.labs.lodestar.utils.TokenUtils;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.ws.rs.WebApplicationException;
import java.util.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@Tag("nested")
class EngagementResourceGetTest extends IntegrationTestHelper {

    static String validToken =  TokenUtils.generateTokenString("/JwtClaimsWriter.json");

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

        Mockito.when(engagementApiClient.getEngagements(0, 500)).thenReturn(Collections.emptyList());

        // GET engagement
        Response response = 
        given()
            .when()
                .auth()
                .oauth2(validToken)
                .contentType(ContentType.JSON)
                .get("/engagements");

        assertEquals(200, response.getStatusCode());
        Engagement[] engagements = response.getBody().as(Engagement[].class);
        assertEquals(0, engagements.length);

        Mockito.verify(engagementApiClient).getEngagements(0, 500);
    }

    @Test
    void testGetEngagementsSuccess() {

        Engagement engagement1 = Engagement.builder().uuid("1234").type("Residency").customerName("Customer").projectName("Project1").build();
        Engagement engagement2 = Engagement.builder().uuid("1235").type("Residency").customerName("Customer").projectName("Project2").build();

        Mockito.when(engagementApiClient.getEngagements(0, 500)).thenReturn(List.of(engagement1, engagement2));

        // GET engagement
        Response response = 
        given()
            .when()
                .auth()
                .oauth2(validToken)
                .contentType(ContentType.JSON)
                .get("/engagements");

        assertEquals(200, response.getStatusCode());
        Engagement[] engagements = quarkusJsonb.fromJson(response.getBody().asString(), Engagement[].class);
        assertEquals(2, engagements.length);

        Mockito.verify(engagementApiClient).getEngagements(0, 500);
    }

    // TODO Abandoning support for include / exclude in v2.
    // Initial impl will instead not retrieve subcomponents when doing a get engagement list
    // It will only retrieve a full engagement by uuid
    // uuid will default to retrieving everything
    // sub-components are: engagement status, activity, hosting env, participants, artifacts
    @Test
    void testGetAllWithExcludeAndInclude() {

        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json");

        // get all
        Response r =given()
            .auth()
            .oauth2(token)
            .queryParam("include", "somevalue")
            .queryParam("exclude", "anothervalue")
            .contentType(ContentType.JSON)
        .when()
            .get("/engagements");

        assertEquals(200, r.getStatusCode());

    }

    @Test
    void testGetAllWithInclude() {

        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json");

        PagedEngagementResults results = PagedEngagementResults.builder().results(Lists.newArrayList()).build();
        Mockito.when(engagementApiClient.getEngagements(0, 5000)).thenReturn(Collections.emptyList());

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

        // GET
        given()
            .when()
                .auth()
                .oauth2(token)
                .get("/engagements/users/summary")
            .then()
                .statusCode(400);

    }
}