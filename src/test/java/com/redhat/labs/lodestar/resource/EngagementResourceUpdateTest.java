package com.redhat.labs.lodestar.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.compress.utils.Sets;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.EngagementUser;
import com.redhat.labs.lodestar.model.Launch;
import com.redhat.labs.lodestar.utils.IntegrationTestHelper;
import com.redhat.labs.lodestar.utils.MockUtils;
import com.redhat.labs.lodestar.utils.TokenUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@QuarkusTest
@Tag("nested")
class EngagementResourceUpdateTest extends IntegrationTestHelper {

    @Test
    void testPutEngagementWithAuthAndRoleSuccess() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement persisted = MockUtils.mockMinimumEngagement("c1", "e2", "1234");
        persisted.setProjectId(1234);
        persisted.setLastUpdate(ZonedDateTime.now(ZoneId.of("Z")).toString());
        Engagement toUpdate = MockUtils.cloneEngagement(persisted);
        toUpdate.setDescription("testing");

        Mockito.when(eRepository.findByUuid("1234")).thenReturn(Optional.of(persisted));
        Mockito.when(eRepository.updateEngagement(Mockito.any(), Mockito.eq(toUpdate.getLastUpdate()))).thenReturn(Optional.of(toUpdate));

        String body = quarkusJsonb.toJson(toUpdate);

        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/1234")
            .then()
                .statusCode(200)
                .body("customer_name", equalTo(toUpdate.getCustomerName()))
                .body("project_name", equalTo(toUpdate.getProjectName()))
                .body("project_id", equalTo(1234))
                .body("description", equalTo(toUpdate.getDescription()));

    }

    @ParameterizedTest
    @MethodSource("nullEmptyBlankSource")
    void testPutEngagementWithAuthAndRoleInvalidCustomerName(String input) throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement persisted = MockUtils.mockMinimumEngagement("c1", "e2", "1234");
        persisted.setProjectId(1234);
        persisted.setLastUpdate(ZonedDateTime.now(ZoneId.of("Z")).toString());
        Engagement toUpdate = MockUtils.cloneEngagement(persisted);
        toUpdate.setCustomerName(input);
        toUpdate.setDescription("testing");

        Mockito.when(eRepository.findByUuid("1234")).thenReturn(Optional.of(persisted));

        String body = quarkusJsonb.toJson(toUpdate);

        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/1234")
            .then()
                .statusCode(400);

    }

    @ParameterizedTest
    @MethodSource("nullEmptyBlankSource")
    void testPutEngagementWithAuthAndRoleInvalidProjectName(String input) throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement persisted = MockUtils.mockMinimumEngagement("c1", "e2", "1234");
        persisted.setProjectId(1234);
        persisted.setLastUpdate(ZonedDateTime.now(ZoneId.of("Z")).toString());
        Engagement toUpdate = MockUtils.cloneEngagement(persisted);
        toUpdate.setProjectName(input);
        toUpdate.setDescription("testing");

        Mockito.when(eRepository.findByUuid("1234")).thenReturn(Optional.of(persisted));

        String body = quarkusJsonb.toJson(toUpdate);

        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/1234")
            .then()
                .statusCode(400);

    }

    @Test
    void testPutEngagementWithAuthAndRoleDuplicateUsers() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement persisted = MockUtils.mockMinimumEngagement("c1", "e2", "1234");
        persisted.setProjectId(1234);
        persisted.setLastUpdate(ZonedDateTime.now(ZoneId.of("Z")).toString());
        Engagement toUpdate = MockUtils.cloneEngagement(persisted);
        toUpdate.setDescription("testing");

        // add duplicate users
        EngagementUser user1 = MockUtils.mockEngagementUser("bs@example.com", "bill", "smith", "dev", "1234", false);
        EngagementUser user2 = MockUtils.mockEngagementUser("bs@example.com", "bill", "smith", "dev", "1234", false);
        EngagementUser user3 = MockUtils.mockEngagementUser("jj@example.com", "john", "jones", "dev", "1234", false);
        toUpdate.setEngagementUsers(Sets.newHashSet(user1, user2, user3));

        Mockito.when(eRepository.findByUuid("1234")).thenReturn(Optional.of(persisted));
        Mockito.when(eRepository.updateEngagement(Mockito.any(), Mockito.eq(toUpdate.getLastUpdate()))).thenReturn(Optional.of(toUpdate));

        String body = quarkusJsonb.toJson(toUpdate);

        Response r =
        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/1234");

        assertEquals(200, r.getStatusCode());
        String responseJson = r.getBody().asString();
        Engagement updatedEngagement = quarkusJsonb.fromJson(responseJson, Engagement.class);
        
        assertEquals(toUpdate.getCustomerName(), updatedEngagement.getCustomerName());
        assertEquals(toUpdate.getProjectName(), updatedEngagement.getProjectName());

        // validate users
        assertEquals(2, updatedEngagement.getEngagementUsers().size());

    }

    @Test
    void testPutEngagementWithAuthAndRoleEngagementDoesNotExist() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement toUpdate = MockUtils.mockMinimumEngagement("c1", "e2", "1234");
        toUpdate.setDescription("testing");

        Mockito.when(eRepository.findByUuid("1234")).thenReturn(Optional.empty());

        String body = quarkusJsonb.toJson(toUpdate);

        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/1234")
            .then()
                .statusCode(404);

    }

    @Test
    void testLaunchEngagementWithAuthAndRoleSuccess() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement persisted = MockUtils.mockMinimumEngagement("c1", "e2", "1234");
        persisted.setProjectId(1234);
        persisted.setLastUpdate(ZonedDateTime.now(ZoneId.of("Z")).toString());
        Engagement toUpdate = MockUtils.cloneEngagement(persisted);
        toUpdate.setDescription("testing");

        Mockito.when(eRepository.findByUuid("1234")).thenReturn(Optional.of(persisted));
        Mockito.when(eRepository.updateEngagement(Mockito.any(), Mockito.eq(toUpdate.getLastUpdate()))).thenReturn(Optional.of(toUpdate));

        String body = quarkusJsonb.toJson(toUpdate);
        
        // Launch engagement
        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/launch")
             .then()
                 .statusCode(200)
                 .body("launch.launched_date_time", notNullValue())
                 .body("launch.launched_by", equalTo("John Doe"));

    }

    @Test
    void testLaunchEngagementWithAuthAndRoleEngagementAlreadyLaunched() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement toUpdate = MockUtils.mockMinimumEngagement("c1", "e2", "1234");
        toUpdate.setDescription("testing");
        toUpdate.setLaunch(Launch.builder().build());

        Mockito.when(eRepository.findByUuid("1234")).thenReturn(Optional.empty());

        String body = quarkusJsonb.toJson(toUpdate);

        // Launch engagement
        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/launch")
             .then()
                 .statusCode(400);

    }

    @Test
    void testLaunchEngagementWithAuthAndRoleEngagementNotFound() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement toUpdate = MockUtils.mockMinimumEngagement("c1", "e2", "1234");
        toUpdate.setDescription("testing");

        Mockito.when(eRepository.findByUuid("1234")).thenReturn(Optional.empty());

        String body = quarkusJsonb.toJson(toUpdate);

        // Launch engagement
        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/launch")
             .then()
                 .statusCode(404);

    }

    @Test
    void testPutEngagementWithConflictingHostingEvironmentSubdomain() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement persisted = MockUtils.mockMinimumEngagement("c1", "e2", "1234");
        persisted.setProjectId(1234);
        persisted.setLastUpdate(ZonedDateTime.now(ZoneId.of("Z")).toString());
        Engagement toUpdate = MockUtils.cloneEngagement(persisted);
        toUpdate.setDescription("testing");

        Mockito.when(eRepository.findByUuid("1234")).thenReturn(Optional.of(persisted));
        Mockito.when(eRepository.findBySubdomain("s", Optional.of("1234"))).thenReturn(Optional.empty());
        Mockito.when(eRepository.findBySubdomain("s")).thenReturn(Optional.of(MockUtils.mockMinimumEngagement("c5","e4", "9876")));

        String body = quarkusJsonb.toJson(toUpdate);

        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/1234")
            .then()
                .statusCode(409);

    }


    @Test
    void testPutEngagementByNamesWithAuthAndRoleSuccess() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement persisted = MockUtils.mockMinimumEngagement("c1", "e2", "1234");
        persisted.setProjectId(1234);
        persisted.setLastUpdate(ZonedDateTime.now(ZoneId.of("Z")).toString());
        Engagement toUpdate = MockUtils.cloneEngagement(persisted);
        toUpdate.setUuid(null);
        toUpdate.setDescription("testing");

        Mockito.when(eRepository.findByCustomerNameAndProjectName("c1", "e2")).thenReturn(Optional.of(persisted));
        Mockito.when(eRepository.updateEngagement(Mockito.any(), Mockito.eq(toUpdate.getLastUpdate()))).thenReturn(Optional.of(toUpdate));

        String body = quarkusJsonb.toJson(toUpdate);

        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/customers/c1/projects/e2")
            .then()
                .statusCode(200)
                .body("customer_name", equalTo(toUpdate.getCustomerName()))
                .body("project_name", equalTo(toUpdate.getProjectName()))
                .body("project_id", equalTo(1234))
                .body("description", equalTo(toUpdate.getDescription()));

    }

    @Test
    void testPutSetUuidsWithAuthAndRoleSuccess() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Mockito.when(eRepository.streamAll()).thenReturn(Stream.of());

        given()
            .when()
                .auth()
                .oauth2(token)
                .contentType(ContentType.JSON)
                .put("/engagements/uuids/set")
            .then()
                .statusCode(200);

    }
    
}