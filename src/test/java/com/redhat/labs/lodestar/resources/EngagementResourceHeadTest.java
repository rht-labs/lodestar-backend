package com.redhat.labs.lodestar.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

import java.util.HashMap;
import java.util.Optional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.utils.MockUtils;
import com.redhat.labs.lodestar.utils.TokenUtils;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@Tag("integration")
class EngagementResourceHeadTest extends EngagementResourceTestHelper {

    @Test
    void testReturnOkIfSubdomainExists() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        String subdomain = "asuperrandomsubdomain";
        Mockito.when(eRepository.findBySubdomain(subdomain)).thenReturn(Optional.empty());

        given().when().auth().oauth2(token).head(String.format("/engagements/subdomain/%s", subdomain)).then()
                .statusCode(200);

    }

    @Test
    void testReturnConflictIfSubdomainExists() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = MockUtils.mockMinimumEngagement("c1", "e1", "1234");
        String subdomain = "asuperrandomsubdomain";
        
        Mockito.when(eRepository.findBySubdomain(subdomain)).thenReturn(Optional.of(engagement));

        given().when().auth().oauth2(token).head(String.format("/engagements/subdomain/%s", subdomain)).then()
                .statusCode(409);

    }

    @Test
    void testHeadEngagementWithAuthAndRoleSuccess() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = MockUtils.mockMinimumEngagement("c1", "e1", "1234");
        engagement.setLastUpdate("somevalue");
        Mockito.when(eRepository.findByUuid("1234", Optional.empty())).thenReturn(Optional.of(engagement));

        // HEAD
        given()
            .when()
                .auth()
                .oauth2(token)
                .head("/engagements/1234")
            .then()
                .statusCode(200)
                .header("last-update", notNullValue())
                .header("Access-Control-Expose-Headers", "last-update");

    }

    @Test
    void testHeadEngagementWithAuthAndRoleNptFound() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Mockito.when(eRepository.findByUuid("1234", Optional.empty())).thenReturn(Optional.empty());

        // HEAD
        given()
            .when()
                .auth()
                .oauth2(token)
                .head("/engagements/1234")
            .then()
                .statusCode(404);

    }
    
}