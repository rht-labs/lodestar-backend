package com.redhat.labs.lodestar.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.rest.client.LodeStarStatusApiClient;
import com.redhat.labs.lodestar.service.EngagementService;
import com.redhat.labs.lodestar.utils.EmbeddedMongoTest;
import com.redhat.labs.lodestar.utils.ResourceLoader;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;

@EmbeddedMongoTest
@QuarkusTest
public class StatusResourceTest {
    
    @Inject
    EngagementService engagementService;
    
    @InjectMock
    @RestClient
    LodeStarStatusApiClient statusClient;
    
    @BeforeEach
    public void seed() {
        Engagement engagement = Engagement.builder().customerName("jello").projectName("exists").build();
        engagementService.create(engagement);
    }
    
    
    @Test
    public void testStatusValid() {
        
        String body = ResourceLoader.load("StatusReqValid.json");
                
        given()
        .when()
            .contentType(ContentType.JSON)
            .body(body)
            .header("x-gitlab-token", "ttttt")
            .post("/status/hook")
        .then()
            .statusCode(200);
    } 
    
    @Test
    public void testStatusNoStatusUpdate() {
        
        String body = ResourceLoader.load("StatusReqValidNoUpdate.json");
        
        given()
        .when()
            .contentType(ContentType.JSON)
            .body(body)
            .header("x-gitlab-token", "ttttt")
            .post("/status/hook")
        .then()
            .statusCode(200);
    } 

    @Test
    public void testStatusNoToken() {
        given()
        .when()
            .contentType(ContentType.JSON)
            .post("/status/hook")
        .then()
            .statusCode(401);
    } 
    
    @Test
    public void testDeletedHook() {
        String body = ResourceLoader.load("StatusDeleted.json");
        
        given()
        .when()
            .contentType(ContentType.JSON)
            .body(body)
            .header("x-notification-token", "CLEANUP")
            .post("/status/deleted")
        .then()
            .statusCode(204);
    }
    
    @Test
    public void testDeletedWrongEventType() {
        String body = ResourceLoader.load("StatusReqValid.json");
        
        given()
        .when()
            .contentType(ContentType.JSON)
            .body(body)
            .header("x-notification-token", "CLEANUP")
            .post("/status/deleted")
        .then()
            .statusCode(200);
    }
    
    @Test
    public void testDeletedNoEngagement() {
        String body = ResourceLoader.load("StatusDeletedNoEngagement.json");
        
        given()
        .when()
            .contentType(ContentType.JSON)
            .body(body)
            .header("x-notification-token", "CLEANUP")
            .post("/status/deleted")
        .then()
            .statusCode(404);
    }
    
    @Test
    public void testDeletedNoToken() {
        given()
        .when()
            .contentType(ContentType.JSON)
            .post("/status/hook")
        .then()
            .statusCode(401);
    }

    @Test
    void testGetComponentStatusSuccess() {

        String json = "{\"status\":\"UP\", \"checks\": []}";
        Mockito.when(statusClient.getComponentStatus()).thenReturn(Response.ok(json).build());

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

        Mockito.when(statusClient.getComponentStatus()).thenReturn(Response.serverError().build());

        given()
        .when()
            .contentType(ContentType.JSON)
            .get("/status")
        .then()
            .statusCode(500);
    }

    @Test
    void testGetComponentStatusRuntimeException() {

        Mockito.when(statusClient.getComponentStatus()).thenThrow(new RuntimeException("uh-oh"));

        given()
        .when()
            .contentType(ContentType.JSON)
            .get("/status")
        .then()
            .statusCode(500);
    }

}
