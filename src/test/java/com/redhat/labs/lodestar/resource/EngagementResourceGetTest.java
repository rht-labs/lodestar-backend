package com.redhat.labs.lodestar.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

import javax.ws.rs.WebApplicationException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.google.common.collect.Lists;
import com.redhat.labs.lodestar.model.Artifact;
import com.redhat.labs.lodestar.model.Category;
import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.EngagementUserSummary;
import com.redhat.labs.lodestar.model.HostingEnvironment;
import com.redhat.labs.lodestar.model.Score;
import com.redhat.labs.lodestar.model.UseCase;
import com.redhat.labs.lodestar.model.filter.FilterOptions;
import com.redhat.labs.lodestar.model.filter.ListFilterOptions;
import com.redhat.labs.lodestar.model.pagination.PagedArtifactResults;
import com.redhat.labs.lodestar.model.pagination.PagedCategoryResults;
import com.redhat.labs.lodestar.model.pagination.PagedEngagementResults;
import com.redhat.labs.lodestar.model.pagination.PagedHostingEnvironmentResults;
import com.redhat.labs.lodestar.model.pagination.PagedScoreResults;
import com.redhat.labs.lodestar.model.pagination.PagedStringResults;
import com.redhat.labs.lodestar.model.pagination.PagedUseCaseResults;
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
        Mockito.when(eRepository.findByUuid("1234", new FilterOptions())).thenReturn(Optional.of(engagement));

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

        Mockito.when(eRepository.findByUuid("1234", new FilterOptions())).thenReturn(Optional.empty());

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

        PagedEngagementResults results = PagedEngagementResults.builder().results(Lists.newArrayList()).build();
        Mockito.when(eRepository.findPagedEngagements(Mockito.any(ListFilterOptions.class))).thenReturn(results);

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

        ArgumentCaptor<ListFilterOptions> ac = ArgumentCaptor.forClass(ListFilterOptions.class);
        Mockito.verify(eRepository).findPagedEngagements(ac.capture());

        ListFilterOptions captured = ac.getValue();
        assertNull(captured.getInclude());
        assertTrue(captured.getSearch().isEmpty());
        assertTrue(captured.getSortOrder().isEmpty());
        assertTrue(captured.getSortFields().isEmpty());
        assertEquals(1, captured.getPage().get());
        assertEquals(500, captured.getPerPage().get());

    }

    @Test
    void testGetEngagementWithAuthAndRoleSuccessEngagments() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "e2", "1234");
        Engagement e2 = MockUtils.mockMinimumEngagement("c1", "e3", "4321");
        
        PagedEngagementResults results = PagedEngagementResults.builder().results(Lists.newArrayList(e1,e2)).build();
        Mockito.when(eRepository.findPagedEngagements(Mockito.any(ListFilterOptions.class))).thenReturn(results);

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

        ArgumentCaptor<ListFilterOptions> ac = ArgumentCaptor.forClass(ListFilterOptions.class);
        Mockito.verify(eRepository).findPagedEngagements(ac.capture());

        ListFilterOptions captured = ac.getValue();
        assertNull(captured.getInclude());
        assertTrue(captured.getSearch().isEmpty());
        assertTrue(captured.getSortOrder().isEmpty());
        assertTrue(captured.getSortFields().isEmpty());
        assertEquals(1, captured.getPage().get());
        assertEquals(500, captured.getPerPage().get());

    }

    @Test
    void testGetAllWithExcludeAndInclude() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Mockito.when(eRepository.findPagedEngagements(Mockito.any(ListFilterOptions.class))).thenThrow(new WebApplicationException(400));

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

        PagedEngagementResults results = PagedEngagementResults.builder().results(Lists.newArrayList()).build();
        Mockito.when(eRepository.findPagedEngagements(Mockito.any(ListFilterOptions.class))).thenReturn(results);

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
        
        PagedCategoryResults pagedResults = PagedCategoryResults.builder().results(Lists.newArrayList(MockUtils.mockCategory("cat1"))).build();
        Mockito.when(eRepository.findCategories(Mockito.any(ListFilterOptions.class))).thenReturn(pagedResults);
        
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
        
        PagedCategoryResults pagedResults = PagedCategoryResults.builder().results(Lists.newArrayList(MockUtils.mockCategory("sugar"))).build();
        Mockito.when(eRepository.findCategories(Mockito.any(ListFilterOptions.class))).thenReturn(pagedResults);
        
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

        PagedStringResults pagedResults = PagedStringResults.builder().results(Lists.newArrayList("a1","a2")).build();
        Mockito.when(eRepository.findArtifactTypes(Mockito.any(ListFilterOptions.class))).thenReturn(pagedResults);

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
    void testGetArtifactTypes() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        PagedStringResults pagedResults = PagedStringResults.builder().results(Lists.newArrayList("a1")).build();
        Mockito.when(eRepository.findArtifactTypes(Mockito.any(ListFilterOptions.class))).thenReturn(pagedResults);

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
    void testGetArtifacts() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Artifact a1 = MockUtils.mockArtifact("demo 1", "demo", "http://demo-1");
        PagedArtifactResults pagedResults = PagedArtifactResults.builder().results(Arrays.asList(a1)).build();
        Mockito.when(eRepository.findArtifacts(Mockito.any(ListFilterOptions.class))).thenReturn(pagedResults);

        given()
            .auth()
            .oauth2(token)
            .contentType(ContentType.JSON)
        .when()
            .get("/engagements/artifacts")
        .then()
            .statusCode(200)
            .body(containsString("demo 1"))
            .body(containsString("http://demo-1"));

    }

    @Test
    void testGetScores() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Score score = Score.builder().name("score1").value(88.8).build();
        PagedScoreResults pagedResults = PagedScoreResults.builder().results(Arrays.asList(score)).build();
        Mockito.when(eRepository.findScores(Mockito.any(ListFilterOptions.class))).thenReturn(pagedResults);

        given()
            .auth()
            .oauth2(token)
            .contentType(ContentType.JSON)
        .when()
            .get("/engagements/scores")
        .then()
            .statusCode(200)
            .body(containsString("score1"))
            .body(containsString("88.8"));

    }

    @Test
    void testGetHostingEnvironments() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        HostingEnvironment he = MockUtils.mockHostingEnvironment("env1", "env-one");
        PagedHostingEnvironmentResults pagedResults = PagedHostingEnvironmentResults.builder().results(Arrays.asList(he)).build();
        Mockito.when(eRepository.findHostingEnvironments(Mockito.any(ListFilterOptions.class))).thenReturn(pagedResults);

        given()
            .auth()
            .oauth2(token)
            .contentType(ContentType.JSON)
        .when()
            .get("engagements/hosting/environments")
        .then()
            .statusCode(200)
            .body(containsString("env1"))
            .body(containsString("env-one"));

    }

    @Test
    void testGetUseCase() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        UseCase u1 = MockUtils.mockUseCase("usecase1", "use case 1", 0);
        PagedUseCaseResults pagedResults = PagedUseCaseResults.builder().results(Arrays.asList(u1)).build();
        Mockito.when(eRepository.findUseCases(Mockito.any(ListFilterOptions.class))).thenReturn(pagedResults);

        given()
            .auth()
            .oauth2(token)
            .contentType(ContentType.JSON)
        .when()
            .get("engagements/usecases")
        .then()
            .statusCode(200)
            .body(containsString("usecase1"))
            .body(containsString("use case 1"));

    }

    @Test
    void testGetEngagementByNamesWithAuthAndRoleSuccess() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = MockUtils.mockMinimumEngagement("c1", "e1", "1234");
        engagement.setProjectId(1234);
        Mockito.when(eRepository.findByCustomerNameAndProjectName("c1", "e1", new FilterOptions())).thenReturn(Optional.of(engagement));

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

    @Test
    void testGetEngagementUserSummary() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = MockUtils.mockMinimumEngagement("c1", "e1", "1234");
        engagement.setProjectId(1234);
        Mockito.when(eRepository.findByCustomerNameAndProjectName("c1", "e1", new FilterOptions())).thenReturn(Optional.of(engagement));

        EngagementUserSummary summary = EngagementUserSummary.builder().allUsersCount(3).rhUsersCount(1).otherUsersCount(2).build();
        Mockito.when(eRepository.findEngagementUserSummary(Mockito.any(ListFilterOptions.class))).thenReturn(summary);

        // GET
        given()
            .when()
                .auth()
                .oauth2(token)
                .get("/engagements/users/summary")
            .then()
                .statusCode(200)
                .body("all_users_count", equalTo(3))
                .body("rh_users_count", equalTo(1))
                .body("other_users_count", equalTo(2));

    }

}