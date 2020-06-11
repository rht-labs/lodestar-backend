package com.redhat.labs.omp.resources;

import static io.restassured.RestAssured.given;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.omp.service.EngagementService;
import com.redhat.labs.utils.EmbeddedMongoTest;
import com.redhat.labs.utils.ResourceLoader;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@EmbeddedMongoTest
@QuarkusTest
public class StatusResourceTest {
    
    @Inject
    EngagementService engagementService;
    
    
    
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
}
