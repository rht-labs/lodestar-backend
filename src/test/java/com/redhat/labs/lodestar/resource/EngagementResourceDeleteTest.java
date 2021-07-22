package com.redhat.labs.lodestar.resource;

import static io.restassured.RestAssured.given;

import java.util.HashMap;
import java.util.Optional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.Launch;
import com.redhat.labs.lodestar.model.filter.FilterOptions;
import com.redhat.labs.lodestar.utils.IntegrationTestHelper;
import com.redhat.labs.lodestar.utils.MockUtils;
import com.redhat.labs.lodestar.utils.TokenUtils;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@Tag("nested")
class EngagementResourceDeleteTest extends IntegrationTestHelper {

    @Test
    void testDeleteEngagementNotFound() throws Exception {
        
        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Mockito.when(eRepository.findByUuid("1234", new FilterOptions())).thenReturn(Optional.empty());

        // DELETE
        given()
        .when()
            .auth()
            .oauth2(token)
            .delete("/engagements/1234")
        .then()
            .statusCode(404);
        
    }

    @Test
    void testDeleteEngagementAlreadyLaunched() throws Exception {
        
        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement e = MockUtils.mockMinimumEngagement("c1", "e1", "1234");
        e.setLaunch(Launch.builder().build());
        Mockito.when(eRepository.findByUuid("1234",new FilterOptions())).thenReturn(Optional.of(e));

        // DELETE
        given()
        .when()
            .auth()
            .oauth2(token)
            .delete("/engagements/1234")
        .then()
            .statusCode(400);
        
    }
    
    @Test
    void testDeleteEngagementSuccess() throws Exception {
        
        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement e = MockUtils.mockMinimumEngagement("c1", "e1", "1234");
        Mockito.when(eRepository.findByUuid("1234", new FilterOptions())).thenReturn(Optional.of(e));

        // DELETE
        given()
        .when()
            .auth()
            .oauth2(token)
            .delete("/engagements/1234")
        .then()
            .statusCode(202);
        
    }
    
}