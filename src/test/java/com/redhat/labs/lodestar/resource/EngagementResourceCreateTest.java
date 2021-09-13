package com.redhat.labs.lodestar.resource;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.utils.IntegrationTestHelper;
import com.redhat.labs.lodestar.utils.TokenUtils;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import javax.ws.rs.WebApplicationException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.times;

@QuarkusTest
@Tag("nested")
class EngagementResourceCreateTest extends IntegrationTestHelper {

    static String validToken =  TokenUtils.generateTokenString("/JwtClaimsWriter.json");

    @BeforeEach
    void setUp() {
        Map<String, List<String>> rbac = Collections.singletonMap("Residency", Collections.singletonList("writer"));
        Mockito.when(configApiClient.getPermission()).thenReturn(rbac);
    }

    @Test
    void testPostEngagementWithWrongRole() {

        String token = TokenUtils.generateTokenString("/JwtClaimsReader.json");

        Engagement engagement = Engagement.builder().customerName("Customer").projectName("Project").build();

        String body = quarkusJsonb.toJson(engagement);

        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .post("/engagements")
            .then()
                .statusCode(403);

        Mockito.verify(engagementApiClient, times(0)).createEngagement(Mockito.any(Engagement.class));
    }
    
    @Test
    void testPostEngagementWithNoGroup() {

        Engagement engagement = Engagement.builder().customerName("Customer").projectName("Project").uuid("9090").type("DO500")
                .build();
 
        String body = quarkusJsonb.toJson(engagement);

        // POST engagement
        given().when().auth().oauth2(validToken).body(body).contentType(ContentType.JSON).post("/engagements").then()
                .statusCode(403);

        Mockito.verify(engagementApiClient, times(0)).createEngagement(Mockito.any(Engagement.class));
    }

    @Test
    void testPostEngagementSuccess() {

        Engagement engagement = Engagement.builder().customerName("Customer").projectName("Project").type("Residency")
                .build();

        String body = quarkusJsonb.toJson(engagement);

        engagement.setUuid("new-uuid");
        Mockito.when(engagementApiClient.createEngagement(Mockito.any(Engagement.class))).thenReturn(engagement);

        // POST engagement
        given()
            .when()
                .auth()
                .oauth2(validToken)
                .body(body)
                .contentType(ContentType.JSON)
                .post("/engagements")
            .then()
                .statusCode(201)
                .body("customer_name", equalTo(engagement.getCustomerName()))
                .body("project_name", equalTo(engagement.getProjectName()))
                .body("public_reference", equalTo(engagement.getPublicReference()))
                .body("uuid", equalTo("new-uuid"))
                .body("project_id", nullValue());

        Mockito.verify(engagementApiClient).createEngagement(Mockito.any(Engagement.class));
    }

    @ParameterizedTest
    @MethodSource("nullEmptyBlankSource")
    void testPostEngagementInvalidCustomerName(String input) {

        Engagement engagement = Engagement.builder().customerName(input).projectName("Project").type("Residency")
                .build();
        String body = quarkusJsonb.toJson(engagement);

        // POST engagement
        given()
            .when()
                .auth()
                .oauth2(validToken)
                .body(body)
                .contentType(ContentType.JSON)
                .post("/engagements")
            .then()
                .statusCode(400);

        Mockito.verify(engagementApiClient, times(0)).createEngagement(Mockito.any(Engagement.class));

    }

    @ParameterizedTest
    @MethodSource("nullEmptyBlankSource")
    void testPostEngagementInvalidProjectName(String input) {

        Engagement engagement = Engagement.builder().customerName("Customer").projectName(input).type("Residency")
                .build();
        String body = quarkusJsonb.toJson(engagement);

        // POST engagement
        given()
            .when()
                .auth()
                .oauth2(validToken)
                .body(body)
                .contentType(ContentType.JSON)
                .post("/engagements")
            .then()
                .statusCode(400);

        Mockito.verify(engagementApiClient, times(0)).createEngagement(Mockito.any(Engagement.class));

    }

    @Test
    void testPostEngagementAlreadyExists() {

        Engagement engagement = Engagement.builder().customerName("Customer").projectName("Project").type("Residency")
                .build();

        Mockito.when(engagementApiClient.createEngagement(Mockito.any(Engagement.class))).thenThrow(
                new WebApplicationException(409));

        String body = quarkusJsonb.toJson(engagement);

        // POST
        given()
            .when()
                .auth()
                .oauth2(validToken)
                .body(body)
                .contentType(ContentType.JSON)
                .post("/engagements")
            .then()
                .statusCode(409);
    }
}