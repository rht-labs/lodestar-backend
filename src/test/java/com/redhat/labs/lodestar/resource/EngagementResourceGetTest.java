package com.redhat.labs.lodestar.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Optional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import com.google.common.collect.Lists;
import com.redhat.labs.lodestar.model.Category;
import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.utils.IntegrationTestHelper;
import com.redhat.labs.lodestar.utils.MockUtils;
import com.redhat.labs.lodestar.utils.TokenUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@QuarkusTest
@Tag("nested")
class EngagementResourceGetTest extends IntegrationTestHelper {

    @Test
    void testGetEngagementWithAuthAndRoleSuccess() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = MockUtils.mockMinimumEngagement("c1", "e1", "1234");
        engagement.setProjectId(1234);
        Mockito.when(eRepository.findByUuid("1234", Optional.empty())).thenReturn(Optional.of(engagement));

        // GET
        given()
            .when()
                .auth()
                .oauth2(token)
                .get("/engagements/1234")
            .then()
                .statusCode(200)
                .body("customer_name", equalTo(engagement.getCustomerName()))
                .body("project_name", equalTo(engagement.getProjectName()))
                .body("project_id", equalTo(1234));

    }

    @Test
    void testGetEngagementWithAuthAndRoleDoesNotExist() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Mockito.when(eRepository.findByUuid("1234", Optional.empty())).thenReturn(Optional.empty());

        // GET
        given()
            .when()
                .auth()
                .oauth2(token)
                .get("/engagements/1234")
            .then()
                .statusCode(404);

    }

    @Test
    void testGetEngagementWithAuthAndRoleSuccessNoEngagements() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Mockito.when(eRepository.findAll(Optional.empty())).thenReturn(Lists.newArrayList());

        // GET engagement
        Response response = 
        given()
            .when()
                .auth()
                .oauth2(token)
                .contentType(ContentType.JSON)
                .get("/engagements");

        assertEquals(200, response.getStatusCode());
        Engagement[] engagements = response.getBody().as(Engagement[].class);
        assertEquals(0, engagements.length);

        Mockito.verify(eRepository).findAll(Optional.empty());

    }

    @Test
    void testGetEngagementWithAuthAndRoleSuccessEngagments() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "e2", "1234");
        Engagement e2 = MockUtils.mockMinimumEngagement("c1", "e3", "4321");
        
        Mockito.when(eRepository.findAll(Optional.empty())).thenReturn(Lists.newArrayList(e1,e2));

        // GET engagement
        Response response = 
        given()
            .when()
                .auth()
                .oauth2(token)
                .contentType(ContentType.JSON)
                .get("/engagements");

        assertEquals(200, response.getStatusCode());
        Engagement[] engagements = quarkusJsonb.fromJson(response.getBody().asString(), Engagement[].class);
        assertEquals(2, engagements.length);

        Mockito.verify(eRepository).findAll(Optional.empty());

    }

    @Test
    void testGetAllWithExcludeAndInclude() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);
        
        // get all
        Response r =given()
            .auth()
            .oauth2(token)
            .queryParam("include", "somevalue")
            .queryParam("exclude", "anothervalue")
            .contentType(ContentType.JSON)
        .when()
            .get("/engagements");

        assertEquals(400, r.getStatusCode());

    }

    @Test
    void testGetAllWithInclude() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);
        
        // get all
        Response r =given()
            .auth()
            .oauth2(token)
            .queryParam("include", "somevalue")
            .contentType(ContentType.JSON)
        .when()
            .get("/engagements");

        assertEquals(200, r.getStatusCode());
        Engagement[] engagements = r.getBody().as(Engagement[].class);
        assertEquals(0, engagements.length);

    }

    @ParameterizedTest
    @MethodSource("nullEmptyBlankSource")
    void testGetCategories(String input) throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);
        
        Mockito.when(eRepository.findAllCategoryWithCounts()).thenReturn(Lists.newArrayList(MockUtils.mockCategory("cat1")));
        
        // get suggestions
        Response r =given()
            .auth()
            .oauth2(token)
            .queryParam("suggest", input)
            .contentType(ContentType.JSON)
        .when()
            .get("/engagements/categories");

        assertEquals(200, r.getStatusCode());
        Category[] results = r.as(Category[].class);
        assertEquals(1, results.length);

    }

    @Test
    void testGetCategories() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);
        
        Mockito.when(eRepository.findCategorySuggestions("sug")).thenReturn(Lists.newArrayList(MockUtils.mockCategory("sugar")));
        
        // get suggestions
        Response r =given()
            .auth()
            .oauth2(token)
            .queryParam("suggest", "sug")
            .contentType(ContentType.JSON)
        .when()
            .get("/engagements/categories");

        assertEquals(200, r.getStatusCode());
        Category[] results = r.as(Category[].class);
        assertEquals(1, results.length);

    }

    @ParameterizedTest
    @MethodSource("nullEmptyBlankSource")
    void testGetArtifactTypes(String input) throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Mockito.when(eRepository.findAllArtifactTypes()).thenReturn(Lists.newArrayList("a1","a2"));

        given()
            .auth()
            .oauth2(token)
            .contentType(ContentType.JSON)
        .when()
            .get("/engagements/artifact/types")
        .then()
            .statusCode(200)
            .body(containsString("a1"))
            .body(containsString("a2"));

    }
    
    @Test
    void testGetArtifacts() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Mockito.when(eRepository.findArtifactTypeSuggestions("a")).thenReturn(Lists.newArrayList("a1"));

        given()
            .auth()
            .oauth2(token)
            .contentType(ContentType.JSON)
            .queryParam("suggest", "a")
        .when()
            .get("/engagements/artifact/types")
        .then()
            .statusCode(200)
            .body(containsString("a1"));

    }

    @Test
    void testGetEngagementByNamesWithAuthAndRoleSuccess() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = MockUtils.mockMinimumEngagement("c1", "e1", "1234");
        engagement.setProjectId(1234);
        Mockito.when(eRepository.findByCustomerNameAndProjectName("c1", "e1", Optional.empty())).thenReturn(Optional.of(engagement));

        // GET
        given()
            .when()
                .auth()
                .oauth2(token)
                .get("/engagements/customers/c1/projects/e1")
            .then()
                .statusCode(200)
                .body("customer_name", equalTo(engagement.getCustomerName()))
                .body("project_name", equalTo(engagement.getProjectName()))
                .body("project_id", equalTo(1234));

    }

}