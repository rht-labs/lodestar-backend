package com.redhat.labs.lodestar.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.*;

import com.redhat.labs.lodestar.model.HostingEnvironment;
import com.redhat.labs.lodestar.model.filter.ArtifactOptions;
import com.redhat.labs.lodestar.rest.client.*;
import io.quarkus.test.junit.mockito.InjectMock;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.EngagementUser;
import com.redhat.labs.lodestar.model.Launch;
import com.redhat.labs.lodestar.utils.IntegrationTestHelper;
import com.redhat.labs.lodestar.utils.TokenUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

@QuarkusTest
@Tag("nested")
class EngagementResourceUpdateTest extends IntegrationTestHelper {

    @InjectMock
    @RestClient
    HostingEnvironmentApiClient hostingEnvironmentApiClient;

    @InjectMock
    @RestClient
    ParticipantApiClient participantApiClient;

    @InjectMock
    @RestClient
    ArtifactApiClient artifactApiClient;

    @InjectMock
    @RestClient
    CategoryApiClient categoryApiClient;

    @InjectMock
    @RestClient
    ActivityApiClient activityApiClient;

    static String validToken = TokenUtils.generateTokenString("/JwtClaimsWriter.json");
    static String lastUpdate = "2021-04-08T00:00:00.000Z";

    @BeforeEach
    void setUp() {
        Map<String, List<String>> rbac = Collections.singletonMap("Residency", Collections.singletonList("writer"));
        Mockito.when(configApiClient.getPermission()).thenReturn(rbac);
    }
    
    @Test
    void testPutEngagementWithUuidNoGroup() {

        Engagement engagement = Engagement.builder().uuid("1234").customerName("Customer").projectName("Project").type("DO500").build();

        Engagement existing = Engagement.builder().uuid("1234").endDate("different").build();
        Mockito.when(engagementApiClient.getEngagement("1234")).thenReturn(existing);

        String body = quarkusJsonb.toJson(engagement);
        given().when().auth().oauth2(validToken).body(body).contentType(ContentType.JSON).put("/engagements/1234").then()
                .statusCode(403);
    }
    
    @Test
    void testLaunchNoAccessGroup() {

        Engagement engagement = Engagement.builder().uuid("1234").customerName("Customer").projectName("Project").type("DO500").build();

        Engagement existing = Engagement.builder().uuid("1234").endDate("different").build();
        Mockito.when(engagementApiClient.getEngagement("1234")).thenReturn(existing);

        Mockito.when(artifactApiClient.getArtifacts(Mockito.any(ArtifactOptions.class))).thenReturn(Response.ok(Collections.emptyList()).build());

        String body = quarkusJsonb.toJson(engagement);
        given().when().auth().oauth2(validToken).body(body).contentType(ContentType.JSON).put("/engagements/launch").then()
                .statusCode(403);
    }

    @Test
    void testPutEngagementSuccess() {

        Engagement toUpdate = Engagement.builder().uuid("1234").customerName("Customer").projectName("Project").type("Residency")
                .description("testing").lastUpdate(lastUpdate).build();
        String body = quarkusJsonb.toJson(toUpdate);

        Engagement existing = quarkusJsonb.fromJson(body, Engagement.class);
        existing.setDescription(null);

        Mockito.when(engagementApiClient.getEngagement("1234")).thenReturn(existing).thenReturn(toUpdate);
        Mockito.when(engagementApiClient.updateEngagement(Mockito.any(Engagement.class))).thenReturn(Response.ok(toUpdate).build());
        Mockito.when(artifactApiClient.getArtifacts(Mockito.any(ArtifactOptions.class))).thenReturn(Response.ok(Collections.emptyList()).build());

        given()
            .when()
                .auth()
                .oauth2(validToken)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/1234")
            .then()
                .statusCode(200)
                .body("customer_name", equalTo(toUpdate.getCustomerName()))
                .body("project_name", equalTo(toUpdate.getProjectName()))
                .body("description", equalTo(toUpdate.getDescription()))
                .body("last_update", equalTo(toUpdate.getLastUpdate()));

    }

    @ParameterizedTest
    @MethodSource("nullEmptyBlankSource")
    void testPutEngagementWithAuthAndRoleInvalidCustomerName(String input) {

        Engagement toUpdate = Engagement.builder().uuid("1234").customerName(input).projectName("Project").type("Residency")
                .description("testing").build();

        String body = quarkusJsonb.toJson(toUpdate);

        given()
                .when()
                .auth()
                .oauth2(validToken)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/1234")
                .then()
                .statusCode(400);
    }

    @ParameterizedTest
    @MethodSource("nullEmptyBlankSource")
    void testPutEngagementWithAuthAndRoleInvalidProjectName(String input)  {

        Engagement toUpdate = Engagement.builder().uuid("1234").customerName("Customer").projectName(input).type("Residency")
                .description("testing").build();

        String body = quarkusJsonb.toJson(toUpdate);

        given()
            .when()
                .auth()
                .oauth2(validToken)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/1234")
            .then()
                .statusCode(400);

    }

    @Test
    void testPutEngagementWithAuthAndRoleDuplicateUsers() {

        // add duplicate users
        EngagementUser user1 = EngagementUser.builder().email("bs@example.com").firstName("bill").lastName("smith").role("dev").uuid("1234")
                .reset(true).build();

        EngagementUser user2 = EngagementUser.builder().email("bs@example.com").firstName("bill").lastName("smith").role("dev").uuid("1234")
                .reset(true).build();

        EngagementUser user3 = EngagementUser.builder().email("jj@example.com").firstName("john").lastName("jones").role("dev").uuid("1234")
                .reset(true).build();

        Set<EngagementUser> users = new HashSet<>(Arrays.asList(user1, user2, user3));

        assertEquals(2, users.size());

    }

    @Test
    void testPutEngagementDoesNotExist() {

        Engagement toUpdate = Engagement.builder().uuid("1234").customerName("Customer").projectName("Project")
                .type("Residency").lastUpdate(lastUpdate).build();

        Mockito.when(engagementApiClient.getEngagement("1234")).thenThrow(new WebApplicationException(404));
        Mockito.when(activityApiClient.getLastActivity("1234"))
                .thenReturn(Response.ok().header("last-update", lastUpdate).build());

        String body = quarkusJsonb.toJson(toUpdate);

        given()
            .when()
                .auth()
                .oauth2(validToken)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/1234")
            .then()
                .statusCode(404);

    }

    @Test
    void testPutEngagementOutOfSyncLastUpdate() {

        Engagement toUpdate = Engagement.builder().uuid("1234").customerName("Customer").projectName("Project")
                .type("Residency").lastUpdate(lastUpdate).build();

        String body = quarkusJsonb.toJson(toUpdate);

        toUpdate.setLastUpdate(Instant.parse(lastUpdate).plusSeconds(1L).toString());

        Mockito.when(engagementApiClient.getEngagement("1234")).thenReturn(toUpdate);

        given()
                .when()
                .auth()
                .oauth2(validToken)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/1234")
                .then()
                .statusCode(409);
    }

    @Test
    void testLaunchEngagementSuccess() {
        String now = Instant.now().toString();
        Engagement toUpdate = Engagement.builder().uuid("1234").customerName("Customer").projectName("Project").type("Residency")
                .launch(Launch.builder().launchedBy("John Doe").launchedDateTime(now).build()).build();

        Engagement existing = Engagement.builder().uuid(toUpdate.getUuid()).customerName(toUpdate.getCustomerName())
                .projectName(toUpdate.getProjectName()).type(toUpdate.getType()).build();
        existing.setProjectId(1234);
        existing.setLastUpdate(Instant.ofEpochSecond(1).toString());
        toUpdate.setDescription("testing");

        Mockito.when(engagementApiClient.getEngagement("1234")).thenReturn(existing).thenReturn(toUpdate);
        Mockito.when(engagementApiClient.launch("1234", "John Doe", "lodestar-email")).thenReturn(Response.ok(toUpdate).build());
        Mockito.when(artifactApiClient.getArtifacts(Mockito.any(ArtifactOptions.class))).thenReturn(Response.ok(Collections.emptyList()).build());

        String body = quarkusJsonb.toJson(toUpdate);
        
        // Launch engagement
        given()
            .when()
                .auth()
                .oauth2(validToken)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/launch")
             .then()
                 .statusCode(200)
                 .body("launch.launched_date_time", equalTo(now))
                 .body("launch.launched_by", equalTo("John Doe"));
    }

    @Test
    void testLaunchEngagementAlreadyLaunched() {

        Engagement toUpdate = Engagement.builder().uuid("1234").customerName("Customer").projectName("Project").type("Residency")
                .description("testing").build();

        Mockito.when(engagementApiClient.getEngagement("1234")).thenReturn(toUpdate);
        Mockito.when(artifactApiClient.getArtifacts(Mockito.any(ArtifactOptions.class))).thenReturn(Response.ok(Collections.emptyList()).build());
        Mockito.when(engagementApiClient.launch("1234", "John Doe", "lodestar-email"))
                .thenThrow(new WebApplicationException(400));

        String body = quarkusJsonb.toJson(toUpdate);

        // Launch engagement
        given()
            .when()
                .auth()
                .oauth2(validToken)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/launch")
             .then()
                 .statusCode(400);

    }

    @Test
    void testLaunchEngagementNotFound() {

        String now = Instant.now().toString();
        Engagement toUpdate = Engagement.builder().uuid("1234").customerName("Customer").projectName("Project").type("Residency")
                .launch(Launch.builder().launchedBy("John Doe").launchedDateTime(now).build()).build();

        Engagement existing = Engagement.builder().uuid(toUpdate.getUuid()).customerName(toUpdate.getCustomerName())
                .projectName(toUpdate.getProjectName()).type(toUpdate.getType()).build();
        existing.setProjectId(1234);
        existing.setLastUpdate(Instant.ofEpochSecond(1).toString());
        toUpdate.setDescription("testing");

        Mockito.when(engagementApiClient.getEngagement("1234")).thenReturn(existing).thenReturn(toUpdate);
        Mockito.when(artifactApiClient.getArtifacts(Mockito.any(ArtifactOptions.class))).thenReturn(Response.ok(Collections.emptyList()).build());
        Mockito.when(engagementApiClient.launch("1234", "John Doe", "lodestar-email"))
                .thenThrow(new WebApplicationException(404));

        String body = quarkusJsonb.toJson(toUpdate);

        // Launch engagement
        given()
            .when()
                .auth()
                .oauth2(validToken)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/launch")
             .then()
                 .statusCode(404);

    }

    @Test
    void testPutEngagementWithConflictingSubdomain() {

        List<HostingEnvironment> hes = Collections.singletonList(HostingEnvironment.builder().ocpSubDomain("taken").build());
        Engagement toUpdate = Engagement.builder().uuid("1234").customerName("Customer").projectName("Project").name("Project").type("Residency")
                .hostingEnvironments(hes).lastUpdate(lastUpdate).build();

        Mockito.when(engagementApiClient.getEngagement("1234")).thenReturn(toUpdate);
        Mockito.when(hostingEnvironmentApiClient.getHostingEnvironmentsByEngagementUuid("1234")).thenReturn(Collections.emptyList());
        Mockito.when(hostingEnvironmentApiClient.updateHostingEnvironments("1234", hes,"lodestar-email","John Doe"))
                        .thenThrow(new WebApplicationException(409));
        Mockito.when(artifactApiClient.getArtifacts(Mockito.any(ArtifactOptions.class))).thenReturn(Response.ok(Collections.emptyList()).build());
        Mockito.when(activityApiClient.getLastActivity("1234"))
                .thenReturn(Response.ok().header("last-update", lastUpdate).build());

        String body = quarkusJsonb.toJson(toUpdate);

        given()
            .when()
                .auth()
                .oauth2(validToken)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/1234")
            .then()
                .statusCode(409);

    }

    @Test
    void retryGitlabEngagement() {
        Engagement engagement = Engagement.builder().uuid("1234").customerName("Customer").projectName("Project").type("Residency").build();
        Mockito.when(engagementApiClient.getEngagement("1234")).thenReturn(engagement);
        Mockito.when(engagementApiClient.rePushChangesToGitlab("1234", "message")).thenReturn(Response.ok().build());

        given().when().auth().oauth2(validToken).queryParam("uuid", "1234").queryParam("message", "message")
                .put("/engagements/gitlab/repush").then().statusCode(200);

        engagement = Engagement.builder().uuid("1235").customerName("Customer").projectName("Project").type("xxx").build();
        Mockito.when(engagementApiClient.getEngagement("1235")).thenReturn(engagement);

        given().when().auth().oauth2(validToken).queryParam("uuid", "1235").queryParam("message", "message")
                .put("/engagements/gitlab/repush").then().statusCode(403);
    }
    
}