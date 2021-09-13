package com.redhat.labs.lodestar.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.timeout;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Response;

import com.redhat.labs.lodestar.rest.client.ActivityApiClient;
import io.quarkus.test.junit.mockito.InjectMock;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.redhat.labs.lodestar.model.Commit;
import com.redhat.labs.lodestar.utils.TokenUtils;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@Tag("nested")
class ActivityResourceTest {

    static String validToken;

    @InjectMock
    @RestClient
    public ActivityApiClient activityClient;

    @BeforeAll
    static void setUp()  {
        validToken = TokenUtils.generateTokenString("/JwtClaimsWriter.json");
    }
    
    @Test
    void testFetchAllActivityPaged() {
        given().when().auth().oauth2(validToken).get("/engagements/activity").then().statusCode(400);
        
        List<Commit> activity = Collections.singletonList(Commit.builder().id("a1").build());
        Mockito.when(activityClient.getPaginatedActivity(0, 1)).thenReturn(Response.ok().entity(activity).build());
        
        given().queryParam("page", "0").queryParam("pageSize", "1").when().auth().oauth2(validToken).get("/engagements/activity").then().statusCode(200);
        
        Mockito.verify(activityClient, timeout(1000)).getPaginatedActivity(0, 1);
    }

    @Test
    void testFetchActivityByUuid() {
        List<Commit> activity = Collections.singletonList(Commit.builder().id("a1").build());
        Mockito.when(activityClient.getActivityForUuid("1")).thenReturn(activity);

        given().when().auth().oauth2(validToken).get("/engagements/activity/uuid/1").then().statusCode(200).body("[0].id",
                equalTo("a1"));
        
        given().queryParam("page", "3").when().auth().oauth2(validToken).get("/engagements/activity/uuid/1").then().statusCode(200).body("[0].id",
                equalTo("a1"));
        
        Mockito.verify(activityClient, timeout(1000).times(2)).getActivityForUuid("1");
    }
    
    @Test
    void testFetchActivityByUuidAndPage() {
        List<Commit> activity = Collections.singletonList(Commit.builder().id("a1").build());
        Mockito.when(activityClient.getPaginatedActivityForUuid("1", 0, 1)).thenReturn(Response.ok(activity).build());

        given().queryParam("page", "0").queryParam("pageSize", "1").when().auth().oauth2(validToken).get("/engagements/activity/uuid/1").then().statusCode(200).body("[0].id",
                equalTo("a1"));
        
        Mockito.verify(activityClient, timeout(1000)).getPaginatedActivityForUuid("1", 0, 1);
    }

    @Test
    void testFetchActivityByUuidBadRequest() {

        //bad page
        given().queryParam("page", "-1").queryParam("pageSize", "1").when().auth().oauth2(validToken)
                .get("/engagements/activity/uuid/1").then().statusCode(400);
        
        //bad page size
        given().queryParam("page", "1").queryParam("pageSize", "0").when().auth().oauth2(validToken)
        .get("/engagements/activity/uuid/1").then().statusCode(400);
        
        Mockito.verify(activityClient, Mockito.never()).getActivityForUuid("1");
    }
    
    
}
