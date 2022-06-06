package com.redhat.labs.lodestar.resource;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.rest.client.ConfigApiClient;
import com.redhat.labs.lodestar.rest.client.EngagementApiClient;
import com.redhat.labs.lodestar.utils.TokenUtils;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.PropertyNamingStrategy;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
@Tag("nested")
class EngagementResourceJwtTest {

    JsonbConfig config = new JsonbConfig().withFormatting(true)
            .withPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CASE_WITH_UNDERSCORES);
    public Jsonb quarkusJsonb = JsonbBuilder.create(config);

    @InjectMock
    @RestClient
    public ConfigApiClient configApiClient;

    @InjectMock
    @RestClient
    EngagementApiClient engagementApiClient;

    @BeforeEach
    void setUp() {
        Map<String, List<String>> rbac = Collections.singletonMap("Residency", Collections.singletonList("writer"));
        Mockito.when(configApiClient.getPermission()).thenReturn(rbac);
    }

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

        String token = TokenUtils.generateTokenString(claimFile);

        Engagement engagement = Engagement.builder().uuid("uuid").customerName("c11").projectName("e11")
                .type("Residency").build();

        Engagement created = Engagement.builder().uuid("uuid").customerName("c11").projectName("e11")
                        .lastUpdateByName(lastUpdateName).lastUpdateByEmail(lastUpdateEmail).build();

        Mockito.when(engagementApiClient.createEngagement(Mockito.any(Engagement.class))).thenReturn(created);

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
                .body("uuid", equalTo("uuid"))
                .body("customer_name", equalTo(engagement.getCustomerName()))
                .body("project_name", equalTo(engagement.getProjectName()))
                .body("project_id", nullValue())
                .body("last_update_by_name", equalTo(lastUpdateName))
                .body("last_update_by_email", equalTo(lastUpdateEmail));


    }
    
}