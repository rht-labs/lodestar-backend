package com.redhat.labs.lodestar.resource;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.Hook;
import com.redhat.labs.lodestar.model.Status;
import com.redhat.labs.lodestar.rest.client.ActivityApiClient;
import com.redhat.labs.lodestar.rest.client.EngagementApiClient;
import com.redhat.labs.lodestar.rest.client.EngagementStatusApiClient;
import com.redhat.labs.lodestar.rest.client.StatusApiClient;
import com.redhat.labs.lodestar.utils.ResourceLoader;
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

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@Tag("nested")
class StatusResourceTest {

    @InjectMock
    @RestClient
    StatusApiClient statusApiClient;

    @InjectMock
    @RestClient
    EngagementStatusApiClient engagementStatusApiClient;

    @InjectMock
    @RestClient
    EngagementApiClient engagementApiClient;

    @InjectMock
    @RestClient
    ActivityApiClient activityApiClient;

    @BeforeEach
    void setUp() {
        String customer = "jello";
        String exists = "exists";
        String uuid1 = "uuid1";
        Engagement engagement = Engagement.builder().uuid(uuid1).customerName(customer).projectName(exists).build();

        Mockito.when(engagementApiClient.getEngagement(customer, exists)).thenReturn(engagement);
        Mockito.when(engagementApiClient.getEngagement(customer, "doesnotexist")).thenThrow(
                new WebApplicationException(404)
        );

        Status status = Status.builder().status("green").build();
        Mockito.when(engagementStatusApiClient.getEngagementStatus(uuid1)).thenReturn(status);

        Mockito.when(engagementStatusApiClient.updateEngagementStatus(uuid1)).thenReturn(Response.ok().build());
    }

    @Test
    void testStatusValid() {

        String body = ResourceLoader.load("StatusReqValid.json");
                
        given()
        .when()
            .contentType(ContentType.JSON)
            .body(body)
            .header("x-gitlab-token", "ttttt")
            .post("/status/hook")
        .then()
            .statusCode(200);

        Mockito.verify(engagementApiClient).getEngagement("jello", "exists");
        Mockito.verify(engagementStatusApiClient).updateEngagementStatus("uuid1");
        Mockito.verify(engagementStatusApiClient).getEngagementStatus("uuid1");
        Mockito.verify(activityApiClient, Mockito.times(0)).getActivityForUuid("uuid1");
    } 
    
    @Test
    void testStatusActivityUpdate() {

        String body = ResourceLoader.load("StatusReqValidNoUpdate.json");
        
        given()
        .when()
            .contentType(ContentType.JSON)
            .body(body)
            .header("x-gitlab-token", "ttttt")
            .post("/status/hook")
        .then()
            .statusCode(200);

        Mockito.verify(engagementApiClient).getEngagement("jello", "exists");
        Mockito.verify(activityApiClient).postHook(Mockito.any(Hook.class), Mockito.eq("ttttt"));

//        verify(exactly(1), getRequestedFor(urlEqualTo("/api/activity/uuid/uuid1")));

    } 

    @Test
    void testStatusNoToken() {
        given()
        .when()
            .contentType(ContentType.JSON)
            .post("/status/hook")
        .then()
            .statusCode(401);
    } 
    
    @Test
    void testDeletedHook() {

        String body = ResourceLoader.load("StatusDeleted.json");
        
        given()
        .when()
            .contentType(ContentType.JSON)
            .body(body)
            .header("x-notification-token", "CLEANUP")
            .post("/status/deleted")
        .then()
            .statusCode(204);

        Mockito.verify(engagementApiClient).getEngagement("jello", "exists");
        Mockito.verify(engagementApiClient).deleteEngagement("uuid1");
    }
    
    @Test
    void testDeletedNotADeletedEvent() {
        String body = ResourceLoader.load("StatusReqValid.json");
        
        given()
        .when()
            .contentType(ContentType.JSON)
            .body(body)
            .header("x-notification-token", "CLEANUP")
            .post("/status/deleted")
        .then()
            .statusCode(200);

        Mockito.verify(engagementApiClient, Mockito.times(0)).getEngagement("jello", "exists");
        Mockito.verify(engagementApiClient, Mockito.times(0)).deleteEngagement("uuid1");
    }
    
    @Test
    void testDeletedNoEngagement() {
        String body = ResourceLoader.load("StatusDeletedNoEngagement.json");
        
        given()
        .when()
            .contentType(ContentType.JSON)
            .body(body)
            .header("x-notification-token", "CLEANUP")
            .post("/status/deleted")
        .then()
            .statusCode(404);

        Mockito.verify(engagementApiClient, Mockito.times(0)).deleteEngagement(Mockito.anyString());
    }
    
    @Test
    void testDeletedNoToken() {
        given()
        .when()
            .contentType(ContentType.JSON)
            .post("/status/deleted")
        .then()
            .statusCode(401);
    }

    @Test
    void testGetComponentStatusSuccess() {

        String json = "{\"status\":\"UP\", \"checks\": []}";
        Mockito.when(statusApiClient.getComponentStatus()).thenReturn(Response.ok(json).build());

        given()
        .when()
            .contentType(ContentType.JSON)
            .get("/status")
        .then()
            .statusCode(200)
            .body("status", is("UP"));
    }
    @Test
    void testGetComponentStatusErrorResponse() {

        Mockito.when(statusApiClient.getComponentStatus()).thenReturn(Response.serverError().build());

        given()
                .when()
                .contentType(ContentType.JSON)
                .get("/status")
                .then()
                .statusCode(500);
    }

    @Test
    void testGetComponentStatusRuntimeException() {

        Mockito.when(statusApiClient.getComponentStatus()).thenThrow(new RuntimeException("uh-oh"));

        given()
                .when()
                .contentType(ContentType.JSON)
                .get("/status")
                .then()
                .statusCode(500);
    }

}