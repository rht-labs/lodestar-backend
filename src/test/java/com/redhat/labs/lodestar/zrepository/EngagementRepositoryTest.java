package com.redhat.labs.lodestar.zrepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.gradle.internal.impldep.com.google.common.collect.Sets;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;
import com.redhat.labs.lodestar.model.Artifact;
import com.redhat.labs.lodestar.model.Category;
import com.redhat.labs.lodestar.model.Commit;
import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.EngagementUser;
import com.redhat.labs.lodestar.model.EngagementUserSummary;
import com.redhat.labs.lodestar.model.HostingEnvironment;
import com.redhat.labs.lodestar.model.Launch;
import com.redhat.labs.lodestar.model.Status;
import com.redhat.labs.lodestar.model.filter.FilterOptions;
import com.redhat.labs.lodestar.model.filter.ListFilterOptions;
import com.redhat.labs.lodestar.model.filter.SimpleFilterOptions;
import com.redhat.labs.lodestar.model.filter.SortOrder;
import com.redhat.labs.lodestar.model.pagination.PagedCategoryResults;
import com.redhat.labs.lodestar.model.pagination.PagedEngagementResults;
import com.redhat.labs.lodestar.model.pagination.PagedStringResults;
import com.redhat.labs.lodestar.model.search.EngagementState;
import com.redhat.labs.lodestar.repository.EngagementRepository;
import com.redhat.labs.lodestar.utils.EmbeddedMongoTest;
import com.redhat.labs.lodestar.utils.MockUtils;

import io.quarkus.test.junit.QuarkusTest;

@EmbeddedMongoTest
@QuarkusTest
@Tag("integration")
class EngagementRepositoryTest {

    @Inject
    EngagementRepository repository;

    // Set tests:

    // set status

    @Test
    void testSetStatus() {

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "c2", "1234");
        repository.persist(e1);

        Status status = Status.builder().build();
        Optional<Engagement> optional = repository.setStatus("1234", status);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get().getStatus());

    }

    @Test
    void testSetStatusNotFound() {
        assertTrue(repository.setStatus("1234", Status.builder().build()).isEmpty());
    }

    // set commits

    @Test
    void testSetCommits() {

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "c2", "1234");
        repository.persist(e1);

        Commit c = Commit.builder().id("111").build();
        Optional<Engagement> optional = repository.setCommits("1234", Arrays.asList(c));
        assertTrue(optional.isPresent());
        assertNotNull(optional.get().getCommits());
        assertEquals(1, optional.get().getCommits().size());

    }

    @Test
    void testSetCommitsNotFound() {
        assertTrue(repository.setCommits("1234", Arrays.asList(Commit.builder().build())).isEmpty());
    }

    // set project id

    @Test
    void testSetProjectId() {

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "c2", "1234");
        repository.persist(e1);

        Optional<Engagement> optional = repository.setProjectId("1234", 8888);
        assertTrue(optional.isPresent());
        assertEquals(8888, optional.get().getProjectId());

    }

    @Test
    void testSetProjectIdNotFound() {
        assertTrue(repository.setProjectId("1234", 8888).isEmpty());
    }

    // set status
    // set commits
    // update engagement if last update matched

    @Test
    void testUpdateEngagementIfLastUpdateMatched() throws Exception {

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "c2", "1234");
        e1.setLastUpdate("value");
        repository.persist(e1);

        Engagement e2 = MockUtils.cloneEngagement(e1);
        e2.setDescription("testing");

        Optional<Engagement> optional = repository.updateEngagementIfLastUpdateMatched(e2, "value", false);
        assertTrue(optional.isPresent());

    }

    @Test
    void testUpdateEngagementIfLastUpdateMatchedStale() throws Exception {

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "c2", "1234");
        e1.setLastUpdate("value");
        repository.persist(e1);

        Engagement e2 = MockUtils.cloneEngagement(e1);
        e2.setDescription("testing");

        Optional<Engagement> optional = repository.updateEngagementIfLastUpdateMatched(e2, "value2", false);
        assertTrue(optional.isEmpty());

    }

    // Optional<Engagement>:
    // findBySubdomain
    // findBySubdomain with uuid

    @Test
    void testFindBySubdomain() {

        Engagement e = MockUtils.mockMinimumEngagement("c1", "c2", "1234");
        HostingEnvironment he = MockUtils.mockHostingEnvironment("env1", "sub1");
        e.setHostingEnvironments(Lists.newArrayList(he));

        repository.persist(e);

        Optional<Engagement> optional = repository.findBySubdomain("sub1");
        assertTrue(optional.isPresent());
        Engagement persisted = optional.get();
        assertEquals(e.getUuid(), persisted.getUuid());
        assertEquals(e.getCustomerName(), persisted.getCustomerName());
        assertEquals(e.getProjectName(), persisted.getProjectName());

        assertEquals(e.getHostingEnvironments(), persisted.getHostingEnvironments());

    }

    @Test
    void testFindBySubdomainWithUuid() {

        Engagement e = MockUtils.mockMinimumEngagement("c1", "c2", "1234");
        HostingEnvironment he = MockUtils.mockHostingEnvironment("env1", "sub1");
        e.setHostingEnvironments(Lists.newArrayList(he));

        repository.persist(e);

        Optional<Engagement> optional = repository.findBySubdomain("sub1", Optional.of("1234"));
        assertTrue(optional.isPresent());
        Engagement persisted = optional.get();
        assertEquals(e.getUuid(), persisted.getUuid());
        assertEquals(e.getCustomerName(), persisted.getCustomerName());
        assertEquals(e.getProjectName(), persisted.getProjectName());

        assertEquals(e.getHostingEnvironments(), persisted.getHostingEnvironments());

    }

    // find by uuid
    // find by uuid with filter options

    @Test
    void testFindByUuid() {

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "c2", "1234");
        repository.persist(e1);

        Optional<Engagement> optional = repository.findByUuid("1234");
        assertTrue(optional.isPresent());
        Engagement result = optional.get();
        assertNotNull(result.getUuid());
        assertNotNull(result.getCustomerName());
        assertNotNull(result.getProjectName());

    }

    @Test
    void testFindByUuidWithInclude() {

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "c2", "1234");
        repository.persist(e1);

        FilterOptions fo = FilterOptions.builder().include("uuid").build();

        Optional<Engagement> optional = repository.findByUuid("1234", fo);
        assertTrue(optional.isPresent());
        Engagement result = optional.get();
        assertNotNull(result.getUuid());
        assertNull(result.getCustomerName());
        assertNull(result.getProjectName());

    }

    @Test
    void testFindByUuidWithExclude() {

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "c2", "1234");
        repository.persist(e1);

        FilterOptions fo = FilterOptions.builder().exclude("uuid").build();

        Optional<Engagement> optional = repository.findByUuid("1234", fo);
        assertTrue(optional.isPresent());
        Engagement result = optional.get();
        assertNull(result.getUuid());
        assertNotNull(result.getCustomerName());
        assertNotNull(result.getProjectName());

    }

    // find by customer project names
    // find by customer project names with filter options

    @Test
    void testFindByCustomerEngagementNames() {

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "c2", "1234");
        repository.persist(e1);

        Optional<Engagement> optional = repository.findByCustomerNameAndProjectName("c1", "c2");
        assertTrue(optional.isPresent());
        Engagement result = optional.get();
        assertNotNull(result.getUuid());
        assertNotNull(result.getCustomerName());
        assertNotNull(result.getProjectName());

    }

    @Test
    void testFindByCustomerEngagementNamesWithInclude() {

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "c2", "1234");
        repository.persist(e1);

        FilterOptions fo = FilterOptions.builder().include("uuid").build();

        Optional<Engagement> optional = repository.findByCustomerNameAndProjectName("c1", "c2", fo);
        assertTrue(optional.isPresent());
        Engagement result = optional.get();
        assertNotNull(result.getUuid());
        assertNull(result.getCustomerName());
        assertNull(result.getProjectName());

    }

    @Test
    void testFindByCustomerEngagementNamesWithExclude() {

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "c2", "1234");
        repository.persist(e1);

        FilterOptions fo = FilterOptions.builder().exclude("uuid").build();

        Optional<Engagement> optional = repository.findByCustomerNameAndProjectName("c1", "c2", fo);
        assertTrue(optional.isPresent());
        Engagement result = optional.get();
        assertNull(result.getUuid());
        assertNotNull(result.getCustomerName());
        assertNotNull(result.getProjectName());

    }

    // Other:
    // find customer suggestions with list filter options

    @Test
    void testFindCustomerSuggestions() {

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "c2", "1234");
        Engagement e2 = MockUtils.mockMinimumEngagement("e1", "c2", "1234");

        repository.persist(Lists.newArrayList(e1, e2));

        SimpleFilterOptions fo = new SimpleFilterOptions();
        fo.setSuggest("C");
        PagedStringResults pagedResults = repository.findCustomerSuggestions(fo);
        List<String> results = pagedResults.getResults();
        assertEquals(1, results.size());

        fo = new SimpleFilterOptions();
        fo.setSuggest("c");
        pagedResults = repository.findCustomerSuggestions(fo);
        results = pagedResults.getResults();
        assertEquals(1, results.size());

        fo = new SimpleFilterOptions();
        fo.setSuggest("e");
        pagedResults = repository.findCustomerSuggestions(fo);
        results = pagedResults.getResults();
        assertEquals(1, results.size());

        fo = new SimpleFilterOptions();
        fo.setSuggest("1");
        pagedResults = repository.findCustomerSuggestions(fo);
        results = pagedResults.getResults();
        assertEquals(2, results.size());

    }

    // find categories with filter options

    @Test
    void testFindCategorySuggestions() {

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "c2", "1234");
        Category c1 = MockUtils.mockCategory("c1");
        Category c2 = MockUtils.mockCategory("c2");
        e1.setCategories(Lists.newArrayList(c1, c2));

        Engagement e2 = MockUtils.mockMinimumEngagement("e1", "c2", "1234");
        Category c3 = MockUtils.mockCategory("e1");
        Category c4 = MockUtils.mockCategory("e2");
        Category c5 = MockUtils.mockCategory("c2");
        e2.setCategories(Lists.newArrayList(c3, c4, c5));

        repository.persist(Lists.newArrayList(e1, e2));

        SimpleFilterOptions options = new SimpleFilterOptions();
        options.setSuggest("c");
        PagedCategoryResults pagedResults = repository.findCategories(options);
        List<Category> results = pagedResults.getResults();
        assertEquals(2, results.size());

        options = new SimpleFilterOptions();
        options.setSuggest("c2");
        pagedResults = repository.findCategories(options);
        results = pagedResults.getResults();
        assertEquals(1, results.size());

        options = new SimpleFilterOptions();
        options.setSuggest("E");
        pagedResults = repository.findCategories(options);
        results = pagedResults.getResults();
        assertEquals(2, results.size());

    }

    @Test
    void testFindAllCategoryWithCounts() {

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "c2", "1234");
        Category c1 = MockUtils.mockCategory("c1");
        Category c2 = MockUtils.mockCategory("c2");
        e1.setCategories(Lists.newArrayList(c1, c2));

        Engagement e2 = MockUtils.mockMinimumEngagement("e1", "c2", "1234");
        Category c3 = MockUtils.mockCategory("e1");
        Category c4 = MockUtils.mockCategory("e2");
        Category c5 = MockUtils.mockCategory("c2");
        e2.setCategories(Lists.newArrayList(c3, c4, c5));

        repository.persist(Lists.newArrayList(e1, e2));

        PagedCategoryResults pagedResults = repository.findCategories(new SimpleFilterOptions());
        List<Category> results = pagedResults.getResults();
        assertEquals(4, results.size());

        results.stream().forEach(c -> {

            if (c.getName().equals("c1")) {
                assertEquals(1, c.getCount());
            } else if (c.getName().equals("c2")) {
                assertEquals(2, c.getCount());
            } else if (c.getName().equals("e1")) {
                assertEquals(1, c.getCount());
            } else if (c.getName().equals("e2")) {
                assertEquals(1, c.getCount());
            }

        });

    }

    // find artifacts with filter options

    @Test
    void testFindArtifactTypeSuggestions() {

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "c2", "1234");
        Artifact a1 = MockUtils.mockArtifact("a1", "demo", "");
        Artifact a2 = MockUtils.mockArtifact("a2", "status", "");
        e1.setArtifacts(Lists.newArrayList(a1, a2));

        Engagement e2 = MockUtils.mockMinimumEngagement("e1", "c2", "1234");
        Artifact a3 = MockUtils.mockArtifact("a3", "demo", "");
        Artifact a4 = MockUtils.mockArtifact("a4", "video", "");
        e2.setArtifacts(Lists.newArrayList(a3, a4));

        repository.persist(Lists.newArrayList(e1, e2));

        SimpleFilterOptions options = new SimpleFilterOptions();
        options.setSuggest("de");

        PagedStringResults pagedResults = repository.findArtifactTypes(options);
        List<String> results = pagedResults.getResults();
        assertEquals(2, results.size());
        assertTrue(results.contains("demo"));

        options = new SimpleFilterOptions();
        options.setSuggest("V");
        pagedResults = repository.findArtifactTypes(options);
        results = pagedResults.getResults();
        assertEquals(1, results.size());
        assertTrue(results.contains("video"));

        options = new SimpleFilterOptions();
        options.setSuggest("St");
        pagedResults = repository.findArtifactTypes(options);
        results = pagedResults.getResults();
        assertEquals(1, results.size());
        assertTrue(results.contains("status"));

    }

    @Test
    void testFindAllArtifactType() {

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "c2", "1234");
        Artifact a1 = MockUtils.mockArtifact("a1", "demo", "");
        Artifact a2 = MockUtils.mockArtifact("a2", "status", "");
        e1.setArtifacts(Lists.newArrayList(a1, a2));

        Engagement e2 = MockUtils.mockMinimumEngagement("e1", "c2", "1234");
        Artifact a3 = MockUtils.mockArtifact("a3", "demo", "");
        Artifact a4 = MockUtils.mockArtifact("a4", "video", "");
        e2.setArtifacts(Lists.newArrayList(a3, a4));

        repository.persist(Lists.newArrayList(e1, e2));

        PagedStringResults pagedResults = repository.findArtifactTypes(new SimpleFilterOptions());
        List<String> results = pagedResults.getResults();
        assertEquals(3, results.size());
        assertTrue(results.contains("demo"));
        assertTrue(results.contains("video"));
        assertTrue(results.contains("status"));

    }

    // List<Engagement>:
    // findAll with filteroptions

    @Test
    void testFindAll() {

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "c2", "1234");
        repository.persist(e1);

        PagedEngagementResults pagedResults = repository.findPagedEngagements(new ListFilterOptions());
        List<Engagement> results = pagedResults.getResults();
        assertEquals(1, results.size());

    }

    @Test
    void testFindWithInclude() {

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "c2", "1234");
        repository.persist(e1);

        ListFilterOptions fo = new ListFilterOptions();
        fo.setInclude("uuid");

        PagedEngagementResults pagedResults = repository.findPagedEngagements(fo);
        List<Engagement> results = pagedResults.getResults();
        assertEquals(1, results.size());

        Engagement result = results.get(0);
        assertNotNull(result.getUuid());
        assertNull(result.getCustomerName());
        assertNull(result.getProjectName());

    }

    @Test
    void testFindAllWithExclude() {

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "c2", "1234");
        repository.persist(e1);

        ListFilterOptions fo = new ListFilterOptions();
        fo.setExclude("uuid");

        PagedEngagementResults pagedResults = repository.findPagedEngagements(fo);
        List<Engagement> results = pagedResults.getResults();
        assertEquals(1, results.size());

        Engagement result = results.get(0);
        assertNull(result.getUuid());
        assertNotNull(result.getCustomerName());
        assertNotNull(result.getProjectName());

    }

    // list all search ( =, like, exists, not exists)
    @Test
    void testFindAllWithSearchEquals() {

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "c2", "1234");
        repository.persist(e1);

        ListFilterOptions fo = new ListFilterOptions();
        fo.setSearch("customer_name=c1");

        PagedEngagementResults pagedResults = repository.findPagedEngagements(fo);
        List<Engagement> results = pagedResults.getResults();
        assertEquals(1, results.size());

        fo.setSearch("customer_name=C1");
        pagedResults = repository.findPagedEngagements(fo);
        results = pagedResults.getResults();
        assertEquals(0, results.size());

    }

    @Test
    void testFindAllWithSearchLike() {

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "c2", "1234");
        Engagement e2 = MockUtils.mockMinimumEngagement("c2", "c2", "4321");
        repository.persist(e1, e2);

        ListFilterOptions fo = new ListFilterOptions();
        fo.setSearch("customer_name like C");

        PagedEngagementResults pagedResults = repository.findPagedEngagements(fo);
        List<Engagement> results = pagedResults.getResults();
        assertEquals(2, results.size());

        fo.setSearch("customer_name=e");
        pagedResults = repository.findPagedEngagements(fo);
        results = pagedResults.getResults();
        assertEquals(0, results.size());

    }

    @Test
    void testFindAllWithSearchExists() {

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "c2", "1234");
        e1.setLaunch(new Launch());
        repository.persist(e1);

        ListFilterOptions fo = new ListFilterOptions();
        fo.setSearch("launch exists");

        PagedEngagementResults pagedResults = repository.findPagedEngagements(fo);
        List<Engagement> results = pagedResults.getResults();
        assertEquals(1, results.size());

    }

    @Test
    void testFindAllWithSearchNotExists() {

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "c2", "1234");
        repository.persist(e1);

        ListFilterOptions fo = new ListFilterOptions();
        fo.setSearch("not exists launch");

        PagedEngagementResults pagedResults = repository.findPagedEngagements(fo);
        List<Engagement> results = pagedResults.getResults();
        assertEquals(1, results.size());

    }

    // sort order with and without sortFields

    @Test
    void testFindAllWithSortNoFields() {

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "c3", "1234");
        Engagement e2 = MockUtils.mockMinimumEngagement("c2", "c5", "4321");
        Engagement e3 = MockUtils.mockMinimumEngagement("c2", "c4", "1111");
        repository.persist(e1, e2, e3);

        ListFilterOptions fo = new ListFilterOptions();
        fo.setSortOrder(SortOrder.ASC);

        PagedEngagementResults pagedResults = repository.findPagedEngagements(fo);
        List<Engagement> results = pagedResults.getResults();
        assertEquals(3, results.size());

        assertEquals("c1", results.get(0).getCustomerName());
        assertEquals("c3", results.get(0).getProjectName());
        assertEquals("c2", results.get(1).getCustomerName());
        assertEquals("c4", results.get(1).getProjectName());
        assertEquals("c2", results.get(2).getCustomerName());
        assertEquals("c5", results.get(2).getProjectName());

        fo.setSearch("customer_name=e");
        pagedResults = repository.findPagedEngagements(fo);
        results = pagedResults.getResults();
        assertEquals(0, results.size());

        fo = new ListFilterOptions();
        fo.setSortOrder(SortOrder.DESC);

        pagedResults = repository.findPagedEngagements(fo);
        results = pagedResults.getResults();
        assertEquals(3, results.size());

        assertEquals("c2", results.get(0).getCustomerName());
        assertEquals("c5", results.get(0).getProjectName());
        assertEquals("c2", results.get(1).getCustomerName());
        assertEquals("c4", results.get(1).getProjectName());
        assertEquals("c1", results.get(2).getCustomerName());
        assertEquals("c3", results.get(2).getProjectName());

        fo.setSearch("customer_name=e");
        pagedResults = repository.findPagedEngagements(fo);
        results = pagedResults.getResults();
        assertEquals(0, results.size());

    }

    @Test
    void testFindAllWithSortWithFields() {

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "c3", "1234");
        Engagement e2 = MockUtils.mockMinimumEngagement("c2", "c5", "4321");
        Engagement e3 = MockUtils.mockMinimumEngagement("c2", "c4", "1111");
        repository.persist(e1, e2, e3);

        ListFilterOptions fo = new ListFilterOptions();
        fo.setSortOrder(SortOrder.ASC);
        fo.setSortFields("uuid");

        PagedEngagementResults pagedResults = repository.findPagedEngagements(fo);
        List<Engagement> results = pagedResults.getResults();
        assertEquals(3, results.size());

        assertEquals("1111", results.get(0).getUuid());
        assertEquals("1234", results.get(1).getUuid());
        assertEquals("4321", results.get(2).getUuid());

        fo.setSearch("customer_name=e");
        pagedResults = repository.findPagedEngagements(fo);
        results = pagedResults.getResults();
        assertEquals(0, results.size());

        fo = new ListFilterOptions();
        fo.setSortOrder(SortOrder.DESC);
        fo.setSortFields("uuid");

        pagedResults = repository.findPagedEngagements(fo);
        results = pagedResults.getResults();
        assertEquals(3, results.size());

        assertEquals("4321", results.get(0).getUuid());
        assertEquals("1234", results.get(1).getUuid());
        assertEquals("1111", results.get(2).getUuid());

        fo.setSearch("customer_name=e");
        pagedResults = repository.findPagedEngagements(fo);
        results = pagedResults.getResults();
        assertEquals(0, results.size());

    }

    // page without perpage, with perpage

    @Test
    void testFindAllWithPaging() {

        // TODO: check paging header generation

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "c3", "1234");
        Engagement e2 = MockUtils.mockMinimumEngagement("c2", "c5", "4321");
        Engagement e3 = MockUtils.mockMinimumEngagement("c2", "c4", "1111");
        repository.persist(e1, e2, e3);

        ListFilterOptions fo = new ListFilterOptions();
        fo.setPage(1);
        fo.setPerPage(1);

        PagedEngagementResults pagedResults = repository.findPagedEngagements(fo);
        List<Engagement> results = pagedResults.getResults();
        assertEquals(1, results.size());
        assertEquals("c1", results.get(0).getCustomerName());
        assertEquals("c3", results.get(0).getProjectName());

        fo = new ListFilterOptions();
        fo.setPage(2);
        fo.setPerPage(1);

        pagedResults = repository.findPagedEngagements(fo);
        results = pagedResults.getResults();
        assertEquals(1, results.size());
        assertEquals("c2", results.get(0).getCustomerName());
        assertEquals("c4", results.get(0).getProjectName());

        fo = new ListFilterOptions();
        fo.setPage(3);
        fo.setPerPage(1);

        pagedResults = repository.findPagedEngagements(fo);
        results = pagedResults.getResults();
        assertEquals(1, results.size());
        assertEquals("c2", results.get(0).getCustomerName());
        assertEquals("c5", results.get(0).getProjectName());

        // test limit ignored if page set
        fo = new ListFilterOptions();
        fo.setPage(1);
        fo.setPerPage(2);

        pagedResults = repository.findPagedEngagements(fo);
        results = pagedResults.getResults();
        assertEquals(2, results.size());

    }
    
    @Test
    void testGetEngagementsByStateUpcoming() {

        String state = "upcoming";
        String rangeStartDate = "2020-01-01";
        String rangeEndDate = "2020-06-01";

        // start is null
        Engagement e0 = MockUtils.mockMinimumEngagement("c0", "p0", "0000");

        // start before range start
        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "p1", "1111");
        setEngagementState(e1, EngagementState.UPCOMING, rangeStartDate, DateAdjustment.BEFORE);

        // start on range start
        Engagement e2 = MockUtils.mockMinimumEngagement("c2", "p2", "2222");
        setEngagementState(e2, EngagementState.UPCOMING, rangeStartDate, DateAdjustment.ON);

        // start between range start and range end
        Engagement e3 = MockUtils.mockMinimumEngagement("c3", "p3", "3333");
        setEngagementState(e3, EngagementState.UPCOMING, rangeStartDate, DateAdjustment.AFTER);

        // start on range end
        Engagement e4 = MockUtils.mockMinimumEngagement("c4", "p4", "4444");
        setEngagementState(e4, EngagementState.UPCOMING, rangeEndDate, DateAdjustment.ON);

        // start after range end
        Engagement e5 = MockUtils.mockMinimumEngagement("c5", "p5", "5555");
        setEngagementState(e5, EngagementState.UPCOMING, rangeEndDate, DateAdjustment.AFTER);

        repository.persist(e0, e1, e2, e3, e4, e5);

        String searchString = new StringBuilder("state=").append(state).append("&start=").append(rangeStartDate)
                .append("&end=").append(rangeEndDate).toString();
        ListFilterOptions options = new ListFilterOptions();
        options.setSearch(searchString);
        PagedEngagementResults pagedResults = repository.findPagedEngagements(options);
        List<Engagement> results = pagedResults.getResults();
        assertNotNull(results);
        assertEquals(4, results.size());

        List<String> invalidUuids = Arrays.asList("1111", "5555");
        Optional<String> fail = results.stream().map(Engagement::getUuid).filter(u -> invalidUuids.contains(u))
                .findAny();
        if (fail.isPresent()) {
            fail("should not have found UUID in results. " + fail.get());
        }

    }

    @Test
    void testGetEngagementByStateActive() {

        String state = "active";
        String rangeStartDate = "2020-01-01";
        String rangeEndDate = "2020-06-01";

        // engagement end date before range end date
        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "p1", "1111");
        setEngagementState(e1, EngagementState.ACTIVE, rangeEndDate, DateAdjustment.BEFORE);

        // engagement end date on range end date
        Engagement e2 = MockUtils.mockMinimumEngagement("c2", "p2", "2222");
        setEngagementState(e2, EngagementState.ACTIVE, rangeEndDate, DateAdjustment.ON);

        // engagement end date after range end date
        Engagement e3 = MockUtils.mockMinimumEngagement("c3", "p3", "3333");
        setEngagementState(e3, EngagementState.ACTIVE, rangeEndDate, DateAdjustment.AFTER);

        repository.persist(e1, e2, e3);

        String searchString = new StringBuilder("state=").append(state).append("&start=").append(rangeStartDate)
                .append("&end=").append(rangeEndDate).toString();
        ListFilterOptions options = new ListFilterOptions();
        options.setSearch(searchString);
        PagedEngagementResults pagedResults = repository.findPagedEngagements(options);
        List<Engagement> results = pagedResults.getResults();
        assertNotNull(results);
        assertEquals(2, results.size());

        List<String> invalidUuids = Arrays.asList("1111");
        Optional<String> fail = results.stream().map(Engagement::getUuid).filter(u -> invalidUuids.contains(u))
                .findAny();
        if (fail.isPresent()) {
            fail("should not have found UUID in results. " + fail.get());
        }

    }

    @Test
    void testGetEngagementByStatePast() {

        String state = "past";
        String rangeStartDate = "2020-01-01";
        String rangeEndDate = "2020-06-01";

        // engagement end date before range end date
        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "p1", "1111");
        setEngagementState(e1, EngagementState.PAST, rangeEndDate, DateAdjustment.BEFORE);

        // engagement end date on range end date
        Engagement e2 = MockUtils.mockMinimumEngagement("c2", "p2", "2222");
        setEngagementState(e2, EngagementState.PAST, rangeEndDate, DateAdjustment.ON);

        // engagement end date after range end date
        Engagement e3 = MockUtils.mockMinimumEngagement("c3", "p3", "3333");
        setEngagementState(e3, EngagementState.PAST, rangeEndDate, DateAdjustment.AFTER);

        repository.persist(e1, e2, e3);

        String searchString = new StringBuilder("state=").append(state).append("&start=").append(rangeStartDate)
                .append("&end=").append(rangeEndDate).toString();
        ListFilterOptions options = new ListFilterOptions();
        options.setSearch(searchString);
        PagedEngagementResults pagedResults = repository.findPagedEngagements(options);
        List<Engagement> results = pagedResults.getResults();
        assertNotNull(results);
        assertEquals(1, results.size());

        List<String> invalidUuids = Arrays.asList("2222", "3333");
        Optional<String> fail = results.stream().map(Engagement::getUuid).filter(u -> invalidUuids.contains(u))
                .findAny();
        if (fail.isPresent()) {
            fail("should not have found UUID in results. " + fail.get());
        }

    }

    @Test
    void testGetEngagementByStateTerminating() {

        String state = "terminating";
        String rangeStartDate = "2020-01-01";
        String rangeEndDate = "2020-06-01";

        // engagement end date before range end date
        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "p1", "1111");
        setEngagementState(e1, EngagementState.TERMINATING, rangeEndDate, DateAdjustment.BEFORE);

        // engagement end date on range end date
        Engagement e2 = MockUtils.mockMinimumEngagement("c2", "p2", "2222");
        setEngagementState(e2, EngagementState.TERMINATING, rangeEndDate, DateAdjustment.ON);

        // engagement end date after range end date
        Engagement e3 = MockUtils.mockMinimumEngagement("c3", "p3", "3333");
        setEngagementState(e3, EngagementState.TERMINATING, rangeEndDate, DateAdjustment.AFTER);

        // engagement end date after range end date
        Engagement e4 = MockUtils.mockMinimumEngagement("c4", "p4", "4444");
        String archiveDate = adjustDate(rangeEndDate, DateAdjustment.AFTER);
        setEngagementState(e3, EngagementState.TERMINATING, archiveDate, DateAdjustment.ON);

        // engagement end date after range end date
        Engagement e5 = MockUtils.mockMinimumEngagement("c5", "p5", "5555");
        setEngagementState(e3, EngagementState.TERMINATING, archiveDate, DateAdjustment.AFTER);

        repository.persist(e1, e2, e3, e4, e5);

        String searchString = new StringBuilder("state=").append(state).append("&start=").append(rangeStartDate)
                .append("&end=").append(rangeEndDate).toString();
        ListFilterOptions options = new ListFilterOptions();
        options.setSearch(searchString);
        PagedEngagementResults pagedResults = repository.findPagedEngagements(options);
        List<Engagement> results = pagedResults.getResults();
        assertNotNull(results);
        assertEquals(1, results.size());

        List<String> invalidUuids = Arrays.asList("2222", "3333");
        Optional<String> fail = results.stream().map(Engagement::getUuid).filter(u -> invalidUuids.contains(u))
                .findAny();
        if (fail.isPresent()) {
            fail("should not have found UUID in results. " + fail.get());
        }

    }

//    @Test
//    void testGetEngagementsByState() {
//
//        // upcoming
//        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "p1", "1111");
//        makeEngagementUpcoming(e1, null, null, null, null);
//        setEngagementState(e1, EngagementState.UPCOMING, Optional.empty());
//        // active
//        Engagement e2 = MockUtils.mockMinimumEngagement("c2", "p2", "2222");
//        setEngagementState(e2, EngagementState.ACTIVE, Optional.empty());
//        // past
//        Engagement e3 = MockUtils.mockMinimumEngagement("c3", "p3", "3333");
//        setEngagementState(e3, EngagementState.PAST, Optional.empty());
//        // past/terminating
//        Engagement e4 = MockUtils.mockMinimumEngagement("c4", "p4", "4444");
//        setEngagementState(e4, EngagementState.TERMINATING, Optional.empty());
//        repository.persist(e1, e2, e3, e4);
//
//        // find upcoming
//        ListFilterOptions options = new ListFilterOptions();
//        options.addEqualsSearchCriteria("state", "upcoming");
//        PagedEngagementResults pagedResults = repository.findPagedEngagements(options);
//        List<Engagement> results = pagedResults.getResults();
//        assertNotNull(results);
//        assertEquals(1, results.size());
//        assertEquals("1111", results.get(0).getUuid());
//
//        // find active
//        options = new ListFilterOptions();
//        options.addEqualsSearchCriteria("state", "active");
//        pagedResults = repository.findPagedEngagements(options);
//        results = pagedResults.getResults();
//        assertNotNull(results);
//        assertEquals(1, results.size());
//        assertEquals("2222", results.get(0).getUuid());
//
//        // find terminating
//        options = new ListFilterOptions();
//        options.addEqualsSearchCriteria("state", "terminating");
//        pagedResults = repository.findPagedEngagements(options);
//        results = pagedResults.getResults();
//        assertNotNull(results);
//        assertEquals(1, results.size());
//        assertEquals("4444", results.get(0).getUuid());
//
//        // find active
//        options = new ListFilterOptions();
//        options.addEqualsSearchCriteria("state", "past");
//        pagedResults = repository.findPagedEngagements(options);
//        results = pagedResults.getResults();
//        assertNotNull(results);
//        assertEquals(2, results.size());
//
//    }

    enum DateAdjustment {
        BEFORE, ON, AFTER;
    }

    String adjustDate(String date, DateAdjustment comp) {

        String newDate = null;
        LocalDate ld = LocalDate.parse(date);
        switch (comp) {
        case BEFORE:

            newDate = ld.minusDays(5).toString();
            break;

        case AFTER:
            newDate = ld.plusDays(5).toString();
            break;
        default:
            newDate = ld.toString();
            break;
        }

        return newDate;

    }

    void setEngagementState(Engagement engagement, EngagementState state, String referenceDate,
            DateAdjustment adjustment) {

        String adjusted = adjustDate(referenceDate, adjustment);

        if (!EngagementState.UPCOMING.equals(state)) {

            engagement.setEndDate(adjusted);
            engagement.setLaunch(Launch.builder().build());

            if (EngagementState.TERMINATING.equals(state)) {
                String engagementArchiveDate = adjustDate(referenceDate, DateAdjustment.AFTER);
                engagement.setArchiveDate(engagementArchiveDate);
            }

        } else {
            engagement.setStartDate(adjusted);
        }

    }

    @Test
    void testGetUserSummaryNoFilter() {

        // region 1, one red hat user, one other user
        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "p1", "1111");
        EngagementUser user1 = MockUtils.mockEngagementUser("one@redhat.com", "o", "ne", "developer", "11", false);
        EngagementUser user2 = MockUtils.mockEngagementUser("a@example.com", "a", "a", "developer", "99", false);
        e1.setRegion("r1");
        e1.setEngagementUsers(Sets.newHashSet(user1, user2));

        // region 1, one red hat user, same other user, but capital
        Engagement e2 = MockUtils.mockMinimumEngagement("c2", "p2", "2222");
        EngagementUser user3 = MockUtils.mockEngagementUser("two@redhat.com", "t", "wo", "admin", "22", false);
        EngagementUser user4 = MockUtils.mockEngagementUser("A@example.com", "a", "a", "developer", "88", false);
        e2.setRegion("r1");
        e2.setEngagementUsers(Sets.newHashSet(user3, user4));

        // region 2, one red hat user, one other user
        Engagement e3 = MockUtils.mockMinimumEngagement("c3", "p3", "3333");
        EngagementUser user5 = MockUtils.mockEngagementUser("three@redhat.com", "t", "hree", "developer", "33", false);
        EngagementUser user6 = MockUtils.mockEngagementUser("b@example.com", "b", "b", "developer", "77", false);
        e3.setRegion("r2");
        e3.setEngagementUsers(Sets.newHashSet(user5, user6));

        repository.persist(e1, e2, e3);

        EngagementUserSummary summary = repository.findEngagementUserSummary(ListFilterOptions.builder().build());
        assertNotNull(summary);
        assertEquals(5, summary.getAllUsersCount());
        assertEquals(3, summary.getRhUsersCount());
        assertEquals(2, summary.getOtherUsersCount());

    }

    @Test
    void testGetUserSummaryFiltered() {

        // region 1, one red hat user, one other user
        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "p1", "1111");
        EngagementUser user1 = MockUtils.mockEngagementUser("one@redhat.com", "o", "ne", "developer", "11", false);
        EngagementUser user2 = MockUtils.mockEngagementUser("a@example.com", "a", "a", "developer", "99", false);
        e1.setRegion("r1");
        e1.setEngagementUsers(Sets.newHashSet(user1, user2));

        // region 1, one red hat user, same other user, but capital
        Engagement e2 = MockUtils.mockMinimumEngagement("c2", "p2", "2222");
        EngagementUser user3 = MockUtils.mockEngagementUser("two@redhat.com", "t", "wo", "admin", "22", false);
        EngagementUser user4 = MockUtils.mockEngagementUser("A@example.com", "a", "a", "developer", "88", false);
        e2.setRegion("r1");
        e2.setEngagementUsers(Sets.newHashSet(user3, user4));

        // region 2, one red hat user, one other user
        Engagement e3 = MockUtils.mockMinimumEngagement("c3", "p3", "3333");
        EngagementUser user5 = MockUtils.mockEngagementUser("three@redhat.com", "t", "hree", "developer", "33", false);
        EngagementUser user6 = MockUtils.mockEngagementUser("b@example.com", "b", "b", "developer", "77", false);
        e3.setRegion("r2");
        e3.setEngagementUsers(Sets.newHashSet(user5, user6));

        repository.persist(e1, e2, e3);

        EngagementUserSummary summary = repository
                .findEngagementUserSummary(ListFilterOptions.builder().search("region=r1").build());
        assertNotNull(summary);
        assertEquals(3, summary.getAllUsersCount());
        assertEquals(2, summary.getRhUsersCount());
        assertEquals(1, summary.getOtherUsersCount());

    }

}