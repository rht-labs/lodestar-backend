package com.redhat.labs.lodestar.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.filter.FilterOptions;
import com.redhat.labs.lodestar.service.EngagementService;
import com.redhat.labs.lodestar.utils.IntegrationTestHelper;
import com.redhat.labs.lodestar.utils.ResourceLoader;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@Tag("nested")
class StatusResourceTest extends IntegrationTestHelper {
    
    @Inject
    EngagementService engagementService;
        
    Engagement engagement = Engagement.builder().uuid("uuid1").customerName("jello").projectName("exists").build();
        
    @Test
    void testStatusValid() {

        Mockito.when(eRepository.findByCustomerNameAndProjectName("jello", "exists"))
            .thenReturn(Optional.of(engagement));

        String body = ResourceLoader.load("StatusReqValid.json");
                
        given()
        .when()
            .contentType(ContentType.JSON)
            .body(body)
            .header("x-gitlab-token", "ttttt")
            .post("/status/hook")
        .then()
            .statusCode(200);

        Mockito.verify(gitApiClient, Mockito.timeout(1000)).getStatus("jello", "exists");
        Mockito.verify(activityClient, Mockito.timeout(1000)).getActivityForUuid("uuid1");

    } 
    
    @Test
    void testStatusNoStatusUpdate() {
        
        Mockito.when(eRepository.findByCustomerNameAndProjectName("jello", "exists"))
            .thenReturn(Optional.of(engagement));

        String body = ResourceLoader.load("StatusReqValidNoUpdate.json");
        
        given()
        .when()
            .contentType(ContentType.JSON)
            .body(body)
            .header("x-gitlab-token", "ttttt")
            .post("/status/hook")
        .then()
            .statusCode(200);

        Mockito.verify(gitApiClient, Mockito.times(0)).getStatus("jello", "exists");
        Mockito.verify(activityClient, Mockito.timeout(1000)).getActivityForUuid("uuid1");

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
        
        Mockito.when(eRepository.findByCustomerNameAndProjectName("jello", "exists", new FilterOptions()))
            .thenReturn(Optional.of(engagement));
        String body = ResourceLoader.load("StatusDeleted.json");
        
        given()
        .when()
            .contentType(ContentType.JSON)
            .body(body)
            .header("x-notification-token", "CLEANUP")
            .post("/status/deleted")
        .then()
            .statusCode(204);

        Mockito.verify(eRepository).delete(engagement);

    }
    
    @Test
    void testDeletedWrongEventType() {
        String body = ResourceLoader.load("StatusReqValid.json");
        
        given()
        .when()
            .contentType(ContentType.JSON)
            .body(body)
            .header("x-notification-token", "CLEANUP")
            .post("/status/deleted")
        .then()
            .statusCode(200);

        Mockito.verify(eRepository, Mockito.times(0)).delete(Mockito.any(Engagement.class));
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

        Mockito.verify(eRepository, Mockito.times(0)).delete(Mockito.any(Engagement.class));
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