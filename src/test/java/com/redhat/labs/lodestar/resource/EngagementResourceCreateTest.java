package com.redhat.labs.lodestar.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.HostingEnvironment;
import com.redhat.labs.lodestar.utils.IntegrationTestHelper;
import com.redhat.labs.lodestar.utils.MockUtils;
import com.redhat.labs.lodestar.utils.TokenUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@Tag("nested")
class EngagementResourceCreateTest extends IntegrationTestHelper {

    @Test
    void testPostEngagementWithWrongRole() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsReader.json", timeClaims);

        String body = quarkusJsonb.toJson(MockUtils.mockEngagement());

        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .post("/engagements")
            .then()
                .statusCode(403);

        Mockito.verify(eRepository, Mockito.times(0)).persist(Mockito.any(Engagement.class));
        Mockito.verify(gitApiClient, Mockito.times(0)).createOrUpdateEngagement(Mockito.any(), Mockito.anyString(), Mockito.anyString());

    }

    @Test
    void testPostEngagementWithAuthAndRoleSuccess() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = MockUtils.mockMinimumEngagement("c1", "e1", "9090");
        Mockito.when(gitApiClient.createOrUpdateEngagement(Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(Response.ok(engagement).header("Location", "some/path/to/id/1234").build());

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
                .body("public_reference", equalTo(engagement.getPublicReference()))
                .body("project_id", nullValue());

        Mockito.verify(eRepository).persist(Mockito.any(Engagement.class));
        Mockito.verify(gitApiClient).createOrUpdateEngagement(Mockito.any(), Mockito.anyString(), Mockito.anyString());

    }

    @ParameterizedTest
    @MethodSource("nullEmptyBlankSource")
    void testPostEngagementWithAuthAndRoleInvalidCustomerName(String input) throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = MockUtils.mockMinimumEngagement(input, "p1", null);
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
                .statusCode(400);

        Mockito.verify(eRepository, Mockito.times(0)).persist(Mockito.any(Engagement.class));
        Mockito.verify(gitApiClient, Mockito.times(0)).createOrUpdateEngagement(Mockito.any(), Mockito.anyString(), Mockito.anyString());

    }

    @ParameterizedTest
    @MethodSource("nullEmptyBlankSource")
    void testPostEngagementWithAuthAndRoleInvalidProjectName(String input) throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = MockUtils.mockMinimumEngagement("c1", input, null);
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
                .statusCode(400);

        Mockito.verify(eRepository, Mockito.times(0)).persist(Mockito.any(Engagement.class));
        Mockito.verify(gitApiClient, Mockito.times(0)).createOrUpdateEngagement(Mockito.any(), Mockito.anyString(), Mockito.anyString());

    }

    @Test
    void testPostEngagementWithAuthAndRoleEngagemenntAlreadyExists() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = MockUtils.mockMinimumEngagement("c1", "p1", "1234");
        Mockito.when(eRepository.findByUuid("1234")).thenReturn(Optional.of(engagement));
        String body = quarkusJsonb.toJson(engagement);

        // POST
        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .post("/engagements")
            .then()
                .statusCode(409);

        Mockito.verify(eRepository, Mockito.times(0)).persist(Mockito.any(Engagement.class));
        Mockito.verify(gitApiClient, Mockito.times(0)).createOrUpdateEngagement(Mockito.any(), Mockito.anyString(), Mockito.anyString());

    }

    @Test
    void testEngagementWithSubdomainAlreadyExists() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = MockUtils.mockMinimumEngagement("c1", "e2", "1234");
        HostingEnvironment env = HostingEnvironment.builder().environmentName("e1").ocpSubDomain("aSuperRandomSubdomain").build();
        engagement.setHostingEnvironments(Arrays.asList(env));

        Engagement engagement2 = MockUtils.mockMinimumEngagement("c2", "e1", "5432");
        engagement2.setProjectName("anotherRandomName");
        HostingEnvironment env2 = HostingEnvironment.builder().environmentName("e2").ocpSubDomain("aSuperRandomSubdomain").build();
        engagement2.setHostingEnvironments(Arrays.asList(env2));

        Mockito.when(eRepository.findByUuid("5432")).thenReturn(Optional.empty());
        Mockito.when(eRepository.findBySubdomain("aSuperRandomSubdomain")).thenReturn(Optional.of(engagement));

        String body = quarkusJsonb.toJson(engagement2);

        given().when().auth().oauth2(token).body(body).contentType(ContentType.JSON).post("/engagements").then()
                .statusCode(409);

    }
  
}