package com.redhat.labs.lodestar.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import java.util.HashMap;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.utils.IntegrationTestHelper;
import com.redhat.labs.lodestar.utils.MockUtils;
import com.redhat.labs.lodestar.utils.TokenUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@Tag("nested")
class EngagementResourceJwtTest extends IntegrationTestHelper {

    @ParameterizedTest
    @CsvSource({
        "/jwt/user-claims/JwtClaimsAllWithNameClaim.json,John Doe,jdoe@test.com",
        "/jwt/user-claims/JwtClaimsAllWithNoNameClaim.json,jdoe,jdoe@test.com",
        "/jwt/user-claims/JwtClaimsAllWithEmptyNameClaim.json,jdoe,jdoe@test.com",
        "/jwt/user-claims/JwtClaimsAllWithBlankNameClaim.json,jdoe,jdoe@test.com",
        "/jwt/user-claims/JwtClaimsAllNoNameNoUsername.json,jdoe@test.com,jdoe@test.com",
        "/jwt/user-claims/JwtClaimsAllNoNameEmptyUsername.json,jdoe@test.com,jdoe@test.com",
        "/jwt/user-claims/JwtClaimsAllNoNameBlankUsername.json,jdoe@test.com,jdoe@test.com",
        "/jwt/user-claims/JwtClaimsAllNoNameNoUsernameNoEmail.json,lodestar-email,lodestar-email",
        "/jwt/user-claims/JwtClaimsAllNoNameNoUsernameEmptyEmail.json,lodestar-email,lodestar-email",
        "/jwt/user-claims/JwtClaimsAllNoNameNoUsernameBlankEmail.json,lodestar-email,lodestar-email"
    })
    void testPostEngagementWithAuthAndRoleHasUserClaim(String claimFile, String lastUpdateName, String lastUpdateEmail) throws Exception {

        MockUtils.mockRbac(configApiClient);
        
        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString(claimFile, timeClaims);

        Engagement engagement = MockUtils.mockMinimumEngagement("c1", "e1", null);

        String body = quarkusJsonb.toJson(engagement);

        // POST engagement
        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .post("/engagements")
            .then()
                .statusCode(201)
                .body("customer_name", equalTo(engagement.getCustomerName()))
                .body("project_name", equalTo(engagement.getProjectName()))
                .body("project_id", nullValue())
                .body("last_update_by_name", equalTo(lastUpdateName))
                .body("last_update_by_email", equalTo(lastUpdateEmail));


    }
    
}