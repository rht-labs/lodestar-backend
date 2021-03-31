package com.redhat.labs.lodestar.resource;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.google.common.collect.Lists;
import com.redhat.labs.lodestar.model.filter.ListFilterOptions;
import com.redhat.labs.lodestar.model.filter.SimpleFilterOptions;
import com.redhat.labs.lodestar.model.filter.SortOrder;
import com.redhat.labs.lodestar.model.pagination.PagedCategoryResults;
import com.redhat.labs.lodestar.model.pagination.PagedEngagementResults;
import com.redhat.labs.lodestar.model.pagination.PagedStringResults;
import com.redhat.labs.lodestar.utils.IntegrationTestHelper;
import com.redhat.labs.lodestar.utils.TokenUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@Tag("nested")
class EngagementResourceV2GetTest extends IntegrationTestHelper {

    static String INCLUDE_PARAM = "include";
    static String INCLUDE_VALUE = "customer_name,project_name,uuid";

    static String SEARCH_PARAM = "search";
    static String SEARCH_VALUE = "customer like something";

    static String SORT_ORDER_PARAM = "sortOrder";
    static String SORT_ORDER_VALUE = "ASC";

    static String SORT_FIELDS_PARAM = "sortFields";
    static String SORT_FIELDS_VALUE = "customer_name,project_name";

    static String PAGE_PARAM = "page";
    static Integer PAGE_VALUE = 1;

    static String PER_PAGE_PARAM = "perPage";
    static Integer PER_PAGE_VALUE = 20;

    static String SUGGEST_PARAM = "suggest";
    static String SUGGEST_VALUE = "something";

    @Test
    void testGetEngagementWithAuthAndRoleSuccessEngagmentsAllQueryParams() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        PagedEngagementResults results = PagedEngagementResults.builder().results(Lists.newArrayList()).build();
        Mockito.when(eRepository.findPagedEngagements(Mockito.any(ListFilterOptions.class))).thenReturn(results);

        given()
            .auth()
            .oauth2(token)
            .contentType(ContentType.JSON)
            .params(populateAllListFilterQueryParams())
        .when()
            .get("api/v2/engagements")
        .then()
            .statusCode(200)
            .header("x-per-page", "20")
            .header("x-current-page", "1")
            .header("x-first-page", "1")
            .header("x-last-page", "1")
            .header("link", "<http://localhost:8081/api/v2/engagements>; rel=\"first\"; per_page=\"20\"; page=\"1\"");

        ArgumentCaptor<ListFilterOptions> ac = ArgumentCaptor.forClass(ListFilterOptions.class);
        Mockito.verify(eRepository).findPagedEngagements(ac.capture());

        ListFilterOptions captured = ac.getValue();
        assertEquals(INCLUDE_VALUE, captured.getInclude());
        assertEquals(SEARCH_VALUE, captured.getSearch().get());
        assertEquals(SortOrder.ASC, captured.getSortOrder().get());
        assertEquals(SORT_FIELDS_VALUE, captured.getSortFields().get());
        assertEquals(PAGE_VALUE, captured.getPage().get());
        assertEquals(PER_PAGE_VALUE, captured.getPerPage().get());

    }

    @Test
    void testGetEngagementWithAuthAndRoleSuccessEngagmentsQueryParams() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        PagedEngagementResults results = PagedEngagementResults.builder().results(Lists.newArrayList()).build();
        Mockito.when(eRepository.findPagedEngagements(Mockito.any(ListFilterOptions.class))).thenReturn(results);

        given()
            .auth()
            .oauth2(token)
            .contentType(ContentType.JSON)
        .when()
            .get("api/v2/engagements")
        .then()
            .statusCode(200)
            .header("x-per-page", "20")
            .header("x-current-page", "1")
            .header("x-first-page", "1")
            .header("x-last-page", "1")
            .header("link", "<http://localhost:8081/api/v2/engagements>; rel=\"first\"; per_page=\"20\"; page=\"1\"");

        ArgumentCaptor<ListFilterOptions> ac = ArgumentCaptor.forClass(ListFilterOptions.class);
        Mockito.verify(eRepository).findPagedEngagements(ac.capture());

        ListFilterOptions captured = ac.getValue();
        assertNull(captured.getInclude());
        assertEquals(Optional.empty(), captured.getSearch());
        assertEquals(Optional.empty(), captured.getSortOrder());
        assertEquals(Optional.empty(), captured.getSortFields());
        assertEquals(PAGE_VALUE, captured.getPage().get());
        assertEquals(PER_PAGE_VALUE, captured.getPerPage().get());

    }

    @Test
    void testGetCustomersWithAuthAndRoleSuccessQueryParams() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        PagedStringResults results = PagedStringResults.builder().results(Lists.newArrayList()).build();
        Mockito.when(eRepository.findCustomerSuggestions(Mockito.any(SimpleFilterOptions.class))).thenReturn(results);

        given()
            .auth()
            .oauth2(token)
            .contentType(ContentType.JSON)
            .params(populateSimpleFilterOptions())
        .when()
            .get("api/v2/engagements/customers")
        .then()
            .statusCode(200)
            .header("x-per-page", "20")
            .header("x-current-page", "1")
            .header("x-first-page", "1")
            .header("x-last-page", "1")
            .header("link", "<http://localhost:8081/api/v2/engagements/customers>; rel=\"first\"; per_page=\"20\"; page=\"1\"");

        ArgumentCaptor<SimpleFilterOptions> ac = ArgumentCaptor.forClass(SimpleFilterOptions.class);
        Mockito.verify(eRepository).findCustomerSuggestions(ac.capture());

        SimpleFilterOptions captured = ac.getValue();
        assertEquals(SUGGEST_VALUE, captured.getSuggest());
        assertEquals(PAGE_VALUE, captured.getPage());
        assertEquals(PER_PAGE_VALUE, captured.getPerPage());
        assertEquals(SortOrder.ASC, captured.getSortOrder());

    }
    
    @Test
    void testGetCustomersWithAuthAndRoleSuccessNoQueryParams() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        PagedStringResults results = PagedStringResults.builder().results(Lists.newArrayList()).build();
        Mockito.when(eRepository.findCustomerSuggestions(Mockito.any(SimpleFilterOptions.class))).thenReturn(results);

        given()
            .auth()
            .oauth2(token)
            .contentType(ContentType.JSON)
        .when()
            .get("api/v2/engagements/customers")
        .then()
            .statusCode(200)
            .header("x-per-page", "20")
            .header("x-current-page", "1")
            .header("x-first-page", "1")
            .header("x-last-page", "1")
            .header("link", "<http://localhost:8081/api/v2/engagements/customers>; rel=\"first\"; per_page=\"20\"; page=\"1\"");

        ArgumentCaptor<SimpleFilterOptions> ac = ArgumentCaptor.forClass(SimpleFilterOptions.class);
        Mockito.verify(eRepository).findCustomerSuggestions(ac.capture());

        SimpleFilterOptions captured = ac.getValue();
        assertNull(captured.getSuggest());
        assertNull(captured.getPage());
        assertNull(captured.getPerPage());
        assertNull(captured.getSortOrder());

    }

    @Test
    void testGetArtifactTypesWithAuthAndRoleSuccessQueryParams() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        PagedStringResults results = PagedStringResults.builder().results(Lists.newArrayList()).build();
        Mockito.when(eRepository.findArtifactTypes(Mockito.any(SimpleFilterOptions.class))).thenReturn(results);

        given()
            .auth()
            .oauth2(token)
            .contentType(ContentType.JSON)
            .params(populateSimpleFilterOptions())
        .when()
            .get("api/v2/engagements/artifact/types")
        .then()
            .statusCode(200)
            .header("x-per-page", "20")
            .header("x-current-page", "1")
            .header("x-first-page", "1")
            .header("x-last-page", "1")
            .header("link", "<http://localhost:8081/api/v2/engagements/artifact/types>; rel=\"first\"; per_page=\"20\"; page=\"1\"");

        ArgumentCaptor<SimpleFilterOptions> ac = ArgumentCaptor.forClass(SimpleFilterOptions.class);
        Mockito.verify(eRepository).findArtifactTypes(ac.capture());

        SimpleFilterOptions captured = ac.getValue();
        assertEquals(SUGGEST_VALUE, captured.getSuggest());
        assertEquals(PAGE_VALUE, captured.getPage());
        assertEquals(PER_PAGE_VALUE, captured.getPerPage());
        assertEquals(SortOrder.ASC, captured.getSortOrder());

    }
    
    @Test
    void testGetArtifactTypesWithAuthAndRoleSuccessNoQueryParams() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        PagedStringResults results = PagedStringResults.builder().results(Lists.newArrayList()).build();
        Mockito.when(eRepository.findArtifactTypes(Mockito.any(SimpleFilterOptions.class))).thenReturn(results);

        given()
            .auth()
            .oauth2(token)
            .contentType(ContentType.JSON)
        .when()
            .get("api/v2/engagements/artifact/types")
        .then()
            .statusCode(200)
            .header("x-per-page", "20")
            .header("x-current-page", "1")
            .header("x-first-page", "1")
            .header("x-last-page", "1")
            .header("link", "<http://localhost:8081/api/v2/engagements/artifact/types>; rel=\"first\"; per_page=\"20\"; page=\"1\"");

        ArgumentCaptor<SimpleFilterOptions> ac = ArgumentCaptor.forClass(SimpleFilterOptions.class);
        Mockito.verify(eRepository).findArtifactTypes(ac.capture());

        SimpleFilterOptions captured = ac.getValue();
        assertNull(captured.getSuggest());
        assertNull(captured.getPage());
        assertNull(captured.getPerPage());
        assertNull(captured.getSortOrder());

    }

    @Test
    void testGetCategoriesWithAuthAndRoleSuccessQueryParams() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        PagedCategoryResults results = PagedCategoryResults.builder().results(Lists.newArrayList()).build();
        Mockito.when(eRepository.findCategories(Mockito.any(SimpleFilterOptions.class))).thenReturn(results);

        given()
            .auth()
            .oauth2(token)
            .contentType(ContentType.JSON)
            .params(populateSimpleFilterOptions())
        .when()
            .get("api/v2/engagements/categories")
        .then()
            .statusCode(200)
            .header("x-per-page", "20")
            .header("x-current-page", "1")
            .header("x-first-page", "1")
            .header("x-last-page", "1")
            .header("link", "<http://localhost:8081/api/v2/engagements/categories>; rel=\"first\"; per_page=\"20\"; page=\"1\"");

        ArgumentCaptor<SimpleFilterOptions> ac = ArgumentCaptor.forClass(SimpleFilterOptions.class);
        Mockito.verify(eRepository).findCategories(ac.capture());

        SimpleFilterOptions captured = ac.getValue();
        assertEquals(SUGGEST_VALUE, captured.getSuggest());
        assertEquals(PAGE_VALUE, captured.getPage());
        assertEquals(PER_PAGE_VALUE, captured.getPerPage());
        assertEquals(SortOrder.ASC, captured.getSortOrder());

    }
    
    @Test
    void testGetCategoriesWithAuthAndRoleSuccessNoQueryParams() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        PagedCategoryResults results = PagedCategoryResults.builder().results(Lists.newArrayList()).build();
        Mockito.when(eRepository.findCategories(Mockito.any(SimpleFilterOptions.class))).thenReturn(results);

        given()
            .auth()
            .oauth2(token)
            .contentType(ContentType.JSON)
        .when()
            .get("api/v2/engagements/categories")
        .then()
            .statusCode(200)
            .header("x-per-page", "20")
            .header("x-current-page", "1")
            .header("x-first-page", "1")
            .header("x-last-page", "1")
            .header("link", "<http://localhost:8081/api/v2/engagements/categories>; rel=\"first\"; per_page=\"20\"; page=\"1\"");

        ArgumentCaptor<SimpleFilterOptions> ac = ArgumentCaptor.forClass(SimpleFilterOptions.class);
        Mockito.verify(eRepository).findCategories(ac.capture());

        SimpleFilterOptions captured = ac.getValue();
        assertNull(captured.getSuggest());
        assertNull(captured.getPage());
        assertNull(captured.getPerPage());
        assertNull(captured.getSortOrder());

    }

    @Test
    void testGetEngagementsByState() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        PagedEngagementResults results = PagedEngagementResults.builder().results(Lists.newArrayList()).build();
        Mockito.when(eRepository.findPagedEngagements(Mockito.any(ListFilterOptions.class))).thenReturn(results);

        given()
            .auth()
            .oauth2(token)
            .contentType(ContentType.JSON)
        .when()
            .get("api/v2/engagements/state/active")
        .then()
            .statusCode(200)
            .header("x-per-page", "20")
            .header("x-current-page", "1")
            .header("x-first-page", "1")
            .header("x-last-page", "1")
            .header("link", "<http://localhost:8081/api/v2/engagements/state/active>; rel=\"first\"; per_page=\"20\"; page=\"1\"");

        ArgumentCaptor<ListFilterOptions> ac = ArgumentCaptor.forClass(ListFilterOptions.class);
        Mockito.verify(eRepository).findPagedEngagements(ac.capture());

        ListFilterOptions captured = ac.getValue();
        assertNull(captured.getInclude());
        assertEquals("state=active", captured.getSearch().get());
        assertEquals(Optional.empty(), captured.getSortOrder());
        assertEquals(Optional.empty(), captured.getSortFields());
        assertEquals(PAGE_VALUE, captured.getPage().get());
        assertEquals(PER_PAGE_VALUE, captured.getPerPage().get());

    }

    @Test
    void testGetEngagementsByStateWithToday() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        PagedEngagementResults results = PagedEngagementResults.builder().results(Lists.newArrayList()).build();
        Mockito.when(eRepository.findPagedEngagements(Mockito.any(ListFilterOptions.class))).thenReturn(results);

        String date = LocalDate.now().toString();

        given()
            .auth()
            .oauth2(token)
            .contentType(ContentType.JSON)
            .param("today", date)
        .when()
            .get("api/v2/engagements/state/active")
        .then()
            .statusCode(200)
            .header("x-per-page", "20")
            .header("x-current-page", "1")
            .header("x-first-page", "1")
            .header("x-last-page", "1")
            .header("link", "<http://localhost:8081/api/v2/engagements/state/active>; rel=\"first\"; per_page=\"20\"; page=\"1\"");

        ArgumentCaptor<ListFilterOptions> ac = ArgumentCaptor.forClass(ListFilterOptions.class);
        Mockito.verify(eRepository).findPagedEngagements(ac.capture());

        ListFilterOptions captured = ac.getValue();
        assertNull(captured.getInclude());
        assertEquals("state=active&today="+date, captured.getSearch().get());
        assertEquals(Optional.empty(), captured.getSortOrder());
        assertEquals(Optional.empty(), captured.getSortFields());
        assertEquals(PAGE_VALUE, captured.getPage().get());
        assertEquals(PER_PAGE_VALUE, captured.getPerPage().get());

    }

    Map<String, Object> populateAllListFilterQueryParams() {

        Map<String, Object> params = new HashMap<>();

        // include
        params.put(INCLUDE_PARAM, INCLUDE_VALUE);

        // search
        params.put(SEARCH_PARAM, SEARCH_VALUE);

        // sortOrder
        params.put(SORT_ORDER_PARAM, SORT_ORDER_VALUE);

        // sortFields
        params.put(SORT_FIELDS_PARAM, SORT_FIELDS_VALUE);

        // page
        params.put(PAGE_PARAM, PAGE_VALUE);

        // perPage
        params.put(PER_PAGE_PARAM, PER_PAGE_VALUE);

        return params;

    }

    Map<String, Object> populateSimpleFilterOptions() {

        Map<String, Object> params = new HashMap<>();

        // page
        params.put(PAGE_PARAM, PAGE_VALUE);

        // perPage
        params.put(PER_PAGE_PARAM, PER_PAGE_VALUE);

        // sortOrder
        params.put(SORT_ORDER_PARAM, SORT_ORDER_VALUE);

        // suggest
        params.put(SUGGEST_PARAM, SUGGEST_VALUE);

        return params;

    }

}
