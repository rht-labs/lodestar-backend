package com.redhat.labs.lodestar.zrepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.LocalDate;
import java.util.ArrayList;
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
import com.redhat.labs.lodestar.model.Score;
import com.redhat.labs.lodestar.model.Status;
import com.redhat.labs.lodestar.model.UseCase;
import com.redhat.labs.lodestar.model.filter.FilterOptions;
import com.redhat.labs.lodestar.model.filter.ListFilterOptions;
import com.redhat.labs.lodestar.model.filter.SortOrder;
import com.redhat.labs.lodestar.model.pagination.PagedArtifactResults;
import com.redhat.labs.lodestar.model.pagination.PagedCategoryResults;
import com.redhat.labs.lodestar.model.pagination.PagedEngagementResults;
import com.redhat.labs.lodestar.model.pagination.PagedHostingEnvironmentResults;
import com.redhat.labs.lodestar.model.pagination.PagedScoreResults;
import com.redhat.labs.lodestar.model.pagination.PagedStringResults;
import com.redhat.labs.lodestar.model.pagination.PagedUseCaseResults;
import com.redhat.labs.lodestar.repository.EngagementRepository;
import com.redhat.labs.lodestar.utils.EmbeddedMongoTest;
import com.redhat.labs.lodestar.utils.MockUtils;

import io.quarkus.test.junit.QuarkusTest;

@EmbeddedMongoTest
@QuarkusTest
@Tag("integration")
class EngagementRepositoryTest {

    static final List<String> ACTIVE_UUIDS = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8");
    static final List<String> UPCOMING_UUIDS = Arrays.asList("9", "10");
    static final List<String> PAST_UUIDS = Arrays.asList("11", "12");
    static final List<String> TERMINATING_UUIDS = Arrays.asList("12");
    static final List<String> RANGE_NO_STATE_UUIDS = Arrays.asList("3", "4", "5", "6", "7", "8");

    static final LocalDate NOW = LocalDate.now();
    String RANGE_START = NOW.minusMonths(1).toString();
    String RANGE_END = NOW.plusMonths(1).toString();

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

        ListFilterOptions fo = new ListFilterOptions();
        fo.setSearch("customer_name like C");
        PagedStringResults pagedResults = repository.findCustomerSuggestions(fo);
        List<String> results = pagedResults.getResults();
        assertEquals(1, results.size());

        fo = new ListFilterOptions();
        fo.setSearch("customer_name like c");
        pagedResults = repository.findCustomerSuggestions(fo);
        results = pagedResults.getResults();
        assertEquals(1, results.size());

        fo = new ListFilterOptions();
        fo.setSearch("customer_name like e");
        pagedResults = repository.findCustomerSuggestions(fo);
        results = pagedResults.getResults();
        assertEquals(1, results.size());

        fo = new ListFilterOptions();
        fo.setSearch("customer_name like 1");
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

        ListFilterOptions options = new ListFilterOptions();
        options.setSearch("categories.name like c");
        PagedCategoryResults pagedResults = repository.findCategories(options);
        List<Category> results = pagedResults.getResults();
        assertEquals(2, results.size());

        options = new ListFilterOptions();
        options.setSearch("categories.name like c2");
        pagedResults = repository.findCategories(options);
        results = pagedResults.getResults();
        assertEquals(1, results.size());

        options = new ListFilterOptions();
        options.setSearch("categories.name like E");
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

        PagedCategoryResults pagedResults = repository.findCategories(new ListFilterOptions());
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

        ListFilterOptions options = new ListFilterOptions();
        options.setSearch("artifacts.type like de");

        PagedStringResults pagedResults = repository.findArtifactTypes(options);
        List<String> results = pagedResults.getResults();
        assertEquals(2, results.size());
        assertTrue(results.contains("demo"));

        options = new ListFilterOptions();
        options.setSearch("artifacts.type like V");
        pagedResults = repository.findArtifactTypes(options);
        results = pagedResults.getResults();
        assertEquals(1, results.size());
        assertTrue(results.contains("video"));

        options = new ListFilterOptions();
        options.setSearch("artifacts.type like St");
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

        PagedStringResults pagedResults = repository.findArtifactTypes(new ListFilterOptions());
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

    @Test
    void testFindByRangeWithNoState() {

        createAndInsertRangeEngagementData();

        String search = "start=" + RANGE_START + "&end=" + RANGE_END;
        ListFilterOptions options = ListFilterOptions.builder().search(search).build();

        PagedEngagementResults results = repository.findPagedEngagements(options);
        assertNotNull(results);
        assertNotNull(results.getResults());

        assertEquals(6, results.getResults().size());
        Optional<String> unknownUuid = results.getResults().stream().map(Engagement::getUuid)
                .filter(u -> !RANGE_NO_STATE_UUIDS.contains(u)).findFirst();
        assertTrue(unknownUuid.isEmpty());

    }

    @Test
    void testFindByStateActiveWithRange() {

        createAndInsertRangeEngagementData();

        String search = "state=active&start=" + RANGE_START + "&end=" + RANGE_END;
        ListFilterOptions options = ListFilterOptions.builder().search(search).build();

        PagedEngagementResults results = repository.findPagedEngagements(options);
        assertNotNull(results);
        assertNotNull(results.getResults());

        assertEquals(8, results.getResults().size());
        Optional<String> unknownUuid = results.getResults().stream().map(Engagement::getUuid)
                .filter(u -> !ACTIVE_UUIDS.contains(u)).findFirst();
        assertTrue(unknownUuid.isEmpty());

    }

    @Test
    void testFindByStateUpcomingWithRange() {

        createAndInsertRangeEngagementData();

        String search = "state=upcoming&start=" + RANGE_START + "&end=" + RANGE_END;
        ListFilterOptions options = ListFilterOptions.builder().search(search).build();

        PagedEngagementResults results = repository.findPagedEngagements(options);
        assertNotNull(results);
        assertNotNull(results.getResults());

        assertEquals(2, results.getResults().size());
        Optional<String> unknownUuid = results.getResults().stream().map(Engagement::getUuid)
                .filter(u -> !UPCOMING_UUIDS.contains(u)).findFirst();
        assertTrue(unknownUuid.isEmpty());

    }

    @Test
    void testFindByStatePastWithRange() {

        createAndInsertRangeEngagementData();

        String search = "state=past&start=" + RANGE_START + "&end=" + RANGE_END;
        ListFilterOptions options = ListFilterOptions.builder().search(search).build();

        PagedEngagementResults results = repository.findPagedEngagements(options);
        assertNotNull(results);
        assertNotNull(results.getResults());

        assertEquals(2, results.getResults().size());

        Optional<String> unknownUuid = results.getResults().stream().map(Engagement::getUuid)
                .filter(u -> !PAST_UUIDS.contains(u)).findFirst();
        assertTrue(unknownUuid.isEmpty());

    }

    @Test
    void testFindByStateTerminatingWithRange() {

        createAndInsertRangeEngagementData();

        String search = "state=terminating&start=" + RANGE_START + "&end=" + RANGE_END;
        ListFilterOptions options = ListFilterOptions.builder().search(search).build();

        PagedEngagementResults results = repository.findPagedEngagements(options);
        assertNotNull(results);
        assertNotNull(results.getResults());

        assertEquals(1, results.getResults().size());
        Optional<String> unknownUuid = results.getResults().stream().map(Engagement::getUuid)
                .filter(u -> !TERMINATING_UUIDS.contains(u)).findFirst();
        assertTrue(unknownUuid.isEmpty());

    }

    @Test
    void testFindArtifactsAll() {

        Artifact a1 = MockUtils.mockArtifact("Demo 1", "demo", "http://demo-1");
        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "p1", "1111");
        e1.setArtifacts(Arrays.asList(a1));

        Artifact a2 = MockUtils.mockArtifact("Report 1", "report", "http://report-1");
        Engagement e2 = MockUtils.mockMinimumEngagement("c2", "p2", "2222");
        e2.setArtifacts(Arrays.asList(a2));

        repository.persist(e1, e2);

        ListFilterOptions options = ListFilterOptions.builder().build();

        PagedArtifactResults pagedResults = repository.findArtifacts(options);
        assertNotNull(pagedResults);
        assertNotNull(pagedResults.getResults());

        List<Artifact> results = pagedResults.getResults();
        assertEquals(2, results.size());

        results.stream().forEach(a -> {

            if ("1111".equals(a.getEngagementUuid())) {
                assertEquals("demo", a.getType());
            } else if ("2222".equals(a.getEngagementUuid())) {
                assertEquals("report", a.getType());
            } else {
                fail("unknown artifact: " + a);
            }

        });

    }

    @Test
    void testFindArtifactsFiltered() {

        Artifact a1 = MockUtils.mockArtifact("Demo 1", "demo", "http://demo-1");
        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "p1", "1111");
        e1.setArtifacts(Arrays.asList(a1));

        Artifact a2 = MockUtils.mockArtifact("Report 1", "report", "http://report-1");
        Engagement e2 = MockUtils.mockMinimumEngagement("c2", "p2", "2222");
        e2.setArtifacts(Arrays.asList(a2));

        repository.persist(e1, e2);

        ListFilterOptions options = ListFilterOptions.builder().search("uuid=1111").build();

        PagedArtifactResults pagedResults = repository.findArtifacts(options);
        assertNotNull(pagedResults);
        assertNotNull(pagedResults.getResults());

        List<Artifact> results = pagedResults.getResults();
        assertEquals(1, results.size());
        assertEquals("demo", results.get(0).getType());

    }

    @Test
    void testFindScoresAll() {

        Score s1 = MockUtils.mockScore("score 1", 88.8);
        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "p1", "1111");
        e1.setScores(Arrays.asList(s1));

        Score s2 = MockUtils.mockScore("score 2", 77.8);
        Engagement e2 = MockUtils.mockMinimumEngagement("c2", "p2", "2222");
        e2.setScores(Arrays.asList(s2));

        repository.persist(e1, e2);

        ListFilterOptions options = ListFilterOptions.builder().build();

        PagedScoreResults pagedResults = repository.findScores(options);
        assertNotNull(pagedResults);
        assertNotNull(pagedResults.getResults());

        List<Score> results = pagedResults.getResults();
        assertEquals(2, results.size());

        results.stream().forEach(a -> {

            if ("1111".equals(a.getEngagementUuid())) {
                assertEquals("score 1", a.getName());
            } else if ("2222".equals(a.getEngagementUuid())) {
                assertEquals("score 2", a.getName());
            } else {
                fail("unknown score: " + a);
            }

        });

    }

    @Test
    void testFindScoresFiltered() {

        Score s1 = MockUtils.mockScore("score 1", 88.8);
        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "p1", "1111");
        e1.setScores(Arrays.asList(s1));

        Score s2 = MockUtils.mockScore("score 2", 77.8);
        Engagement e2 = MockUtils.mockMinimumEngagement("c2", "p2", "2222");
        e2.setScores(Arrays.asList(s2));

        repository.persist(e1, e2);

        ListFilterOptions options = ListFilterOptions.builder().search("uuid=1111").build();

        PagedScoreResults pagedResults = repository.findScores(options);
        assertNotNull(pagedResults);
        assertNotNull(pagedResults.getResults());

        List<Score> results = pagedResults.getResults();
        assertEquals(1, results.size());
        assertEquals("score 1", results.get(0).getName());

    }

    @Test
    void testFindUseCasesAll() {

        UseCase u1 = MockUtils.mockUseCase("case 1", "use case one", 0);
        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "p1", "1111");
        e1.setUseCases(Arrays.asList(u1));

        UseCase u2 = MockUtils.mockUseCase("case 2", "use case two", 0);
        Engagement e2 = MockUtils.mockMinimumEngagement("c2", "p2", "2222");
        e2.setUseCases(Arrays.asList(u2));

        repository.persist(e1, e2);

        ListFilterOptions options = ListFilterOptions.builder().build();

        PagedUseCaseResults pagedResults = repository.findUseCases(options);
        assertNotNull(pagedResults);
        assertNotNull(pagedResults.getResults());

        List<UseCase> results = pagedResults.getResults();
        assertEquals(2, results.size());

        results.stream().forEach(a -> {

            if ("1111".equals(a.getEngagementUuid())) {
                assertEquals("case 1", a.getTitle());
            } else if ("2222".equals(a.getEngagementUuid())) {
                assertEquals("case 2", a.getTitle());
            } else {
                fail("unknown use case: " + a);
            }

        });

    }

    @Test
    void testFindUseCasesFiltered() {

        UseCase u1 = MockUtils.mockUseCase("case 1", "use case one", 0);
        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "p1", "1111");
        e1.setUseCases(Arrays.asList(u1));

        UseCase u2 = MockUtils.mockUseCase("case 2", "use case two", 0);
        Engagement e2 = MockUtils.mockMinimumEngagement("c2", "p2", "2222");
        e2.setUseCases(Arrays.asList(u2));

        repository.persist(e1, e2);

        ListFilterOptions options = ListFilterOptions.builder().search("uuid=1111").build();

        PagedUseCaseResults pagedResults = repository.findUseCases(options);
        assertNotNull(pagedResults);
        assertNotNull(pagedResults.getResults());

        List<UseCase> results = pagedResults.getResults();
        assertEquals(1, results.size());
        assertEquals("case 1", results.get(0).getTitle());

    }

    @Test
    void testFindHostingEnvironmentsAll() {

        HostingEnvironment h1 = MockUtils.mockHostingEnvironment("env 1", "envone");
        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "p1", "1111");
        e1.setHostingEnvironments(Arrays.asList(h1));

        HostingEnvironment h2 = MockUtils.mockHostingEnvironment("env 2", "envtwo");
        Engagement e2 = MockUtils.mockMinimumEngagement("c2", "p2", "2222");
        e2.setHostingEnvironments(Arrays.asList(h2));

        repository.persist(e1, e2);

        ListFilterOptions options = ListFilterOptions.builder().build();

        PagedHostingEnvironmentResults pagedResults = repository.findHostingEnvironments(options);
        assertNotNull(pagedResults);
        assertNotNull(pagedResults.getResults());

        List<HostingEnvironment> results = pagedResults.getResults();
        assertEquals(2, results.size());

        results.stream().forEach(a -> {

            if ("1111".equals(a.getEngagementUuid())) {
                assertEquals("env 1", a.getEnvironmentName());
            } else if ("2222".equals(a.getEngagementUuid())) {
                assertEquals("env 2", a.getEnvironmentName());
            } else {
                fail("unknown use case: " + a);
            }

        });

    }

    @Test
    void testFindHostingEnvironmentsFiltered() {

        HostingEnvironment h1 = MockUtils.mockHostingEnvironment("env 1", "envone");
        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "p1", "1111");
        e1.setHostingEnvironments(Arrays.asList(h1));

        HostingEnvironment h2 = MockUtils.mockHostingEnvironment("env 2", "envtwo");
        Engagement e2 = MockUtils.mockMinimumEngagement("c2", "p2", "2222");
        e2.setHostingEnvironments(Arrays.asList(h2));

        repository.persist(e1, e2);

        ListFilterOptions options = ListFilterOptions.builder().search("uuid=1111").build();

        PagedHostingEnvironmentResults pagedResults = repository.findHostingEnvironments(options);
        assertNotNull(pagedResults);
        assertNotNull(pagedResults.getResults());

        List<HostingEnvironment> results = pagedResults.getResults();
        assertEquals(1, results.size());
        assertEquals("env 1", results.get(0).getEnvironmentName());

    }

    // create test data engagements
    private void createAndInsertRangeEngagementData() {

        List<Engagement> engagements = new ArrayList<>();
        engagements.addAll(mockActiveEngagements());
        engagements.addAll(mockUpcomingEngagements());
        engagements.addAll(mockPastEngagements());
        engagements.addAll(mockTerminatingEngagements());

        repository.persist(engagements);

        assertEquals(12, repository.count());

    }

    private List<Engagement> mockActiveEngagements() {

        // e1 - start date before r start date and end date after r end date and
        // launched and archive date plus 30 days from end date

        Engagement e1 = MockUtils.mockMinimumEngagement("customer1", "project1", "1");
        // before range start date
        String startDate = LocalDate.parse(RANGE_START).minusDays(1).toString();
        e1.setStartDate(startDate);
        // after range end date
        String endDate = LocalDate.parse(RANGE_END).plusDays(1).toString();
        e1.setEndDate(endDate);
        // archive date 30 days after end date
        String archiveDate = LocalDate.parse(endDate).plusDays(30).toString();
        e1.setArchiveDate(archiveDate);
        e1.setLaunch(MockUtils.mockLaunch(startDate, "someone", "someone@example.com"));

        // e4 - start date after r end date and end date after start date and launched
        // and archive date plus 30 days from end date

        Engagement e2 = MockUtils.mockMinimumEngagement("customer2", "project2", "2");
        // after range end date
        startDate = LocalDate.parse(RANGE_END).plusDays(5).toString();
        e2.setStartDate(startDate);
        // after range end date
        endDate = LocalDate.parse(RANGE_END).plusMonths(1).toString();
        e2.setEndDate(endDate);
        // archive date 30 days after end date
        archiveDate = LocalDate.parse(endDate).plusDays(30).toString();
        e2.setArchiveDate(archiveDate);
        e2.setLaunch(MockUtils.mockLaunch(startDate, "someone", "someone@example.com"));

        // e5 - start date on r end date and end date after r end date and launched and
        // archive date plus 30 days from end date

        Engagement e3 = MockUtils.mockMinimumEngagement("customer3", "project3", "3");
        // on range end date
        startDate = LocalDate.parse(RANGE_END).toString();
        e3.setStartDate(startDate);
        // after range end date
        endDate = LocalDate.parse(RANGE_END).plusMonths(1).toString();
        e3.setEndDate(endDate);
        // archive date 30 days after end date
        archiveDate = LocalDate.parse(endDate).plusDays(30).toString();
        e3.setArchiveDate(archiveDate);
        e3.setLaunch(MockUtils.mockLaunch(startDate, "someone", "someone@example.com"));

        // e7 - start date after r start date and end date after r end date and launched
        // and archive date plus 30 days from end date

        Engagement e4 = MockUtils.mockMinimumEngagement("customer4", "project4", "4");
        // before range end date
        startDate = LocalDate.parse(RANGE_END).minusDays(5).toString();
        e4.setStartDate(startDate);
        // after range end date
        endDate = LocalDate.parse(RANGE_END).plusMonths(1).toString();
        e4.setEndDate(endDate);
        // archive date 30 days after end date
        archiveDate = LocalDate.parse(endDate).plusDays(30).toString();
        e4.setArchiveDate(archiveDate);
        e4.setLaunch(MockUtils.mockLaunch(startDate, "someone", "someone@example.com"));

        // e9 - start date before r start date and end date between r start and r end
        // dates and launched

        Engagement e5 = MockUtils.mockMinimumEngagement("customer5", "project5", "5");
        // before range start
        startDate = LocalDate.parse(RANGE_START).minusDays(5).toString();
        e5.setStartDate(startDate);
        // after range end date
        endDate = LocalDate.parse(RANGE_START).plusMonths(1).toString();
        e5.setEndDate(endDate);
        // archive date 30 days after end date
        archiveDate = LocalDate.parse(endDate).plusDays(30).toString();
        e5.setArchiveDate(archiveDate);
        e5.setLaunch(MockUtils.mockLaunch(startDate, "someone", "someone@example.com"));

        // e10- start date on r start date and end date before r end date and launched

        Engagement e6 = MockUtils.mockMinimumEngagement("customer6", "project6", "6");
        // on range start
        startDate = LocalDate.parse(RANGE_START).toString();
        e6.setStartDate(startDate);
        // after range end date
        endDate = LocalDate.parse(RANGE_START).plusMonths(1).toString();
        e6.setEndDate(endDate);
        // archive date 30 days after end date
        archiveDate = LocalDate.parse(endDate).plusDays(20).toString();
        e6.setArchiveDate(archiveDate);
        e6.setLaunch(MockUtils.mockLaunch(startDate, "someone", "someone@example.com"));

        // e11- start date after r start date and end date before r end date and
        // launched

        Engagement e7 = MockUtils.mockMinimumEngagement("customer7", "project7", "7");
        // on range start
        startDate = LocalDate.parse(RANGE_START).plusDays(1).toString();
        e7.setStartDate(startDate);
        // after range end date
        endDate = LocalDate.parse(RANGE_START).plusMonths(1).toString();
        e7.setEndDate(endDate);
        // archive date 30 days after end date
        archiveDate = LocalDate.parse(endDate).plusDays(20).toString();
        e7.setArchiveDate(archiveDate);
        e7.setLaunch(MockUtils.mockLaunch(startDate, "someone", "someone@example.com"));

        // e12- start date after r start date and end date on r end date and launched

        Engagement e8 = MockUtils.mockMinimumEngagement("customer8", "project8", "8");
        // on range start
        startDate = LocalDate.parse(RANGE_START).plusDays(1).toString();
        e8.setStartDate(startDate);
        // on range end date
        endDate = LocalDate.parse(RANGE_END).toString();
        e8.setEndDate(endDate);
        // archive date 30 days after end date
        archiveDate = LocalDate.parse(endDate).plusDays(20).toString();
        e8.setArchiveDate(archiveDate);
        e8.setLaunch(MockUtils.mockLaunch(startDate, "someone", "someone@example.com"));

        return Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8);

    }

    private List<Engagement> mockUpcomingEngagements() {

        // e2 - start date after r start date and end date after r end date and launched
        // and archive date plus 30 days from end date

        Engagement e9 = MockUtils.mockMinimumEngagement("customer9", "project9", "9");
        // before range end
        String startDate = LocalDate.parse(RANGE_END).minusDays(5).toString();
        e9.setStartDate(startDate);
        // after range end date
        String endDate = LocalDate.parse(RANGE_END).plusDays(20).toString();
        e9.setEndDate(endDate);
        // archive date 30 days after end date
        String archiveDate = LocalDate.parse(endDate).plusDays(20).toString();
        e9.setArchiveDate(archiveDate);

        // e3 - start date after r end date and end date after start date and not
        // launched and archive date plus 30 days from end date

        Engagement e10 = MockUtils.mockMinimumEngagement("customer10", "project10", "10");
        // after range end
        startDate = LocalDate.parse(RANGE_END).plusDays(5).toString();
        e10.setStartDate(startDate);
        // after range end date
        endDate = LocalDate.parse(RANGE_END).plusDays(30).toString();
        e10.setEndDate(endDate);
        // archive date 30 days after end date
        archiveDate = LocalDate.parse(endDate).plusDays(20).toString();
        e10.setArchiveDate(archiveDate);

        return Arrays.asList(e9, e10);

    }

    private List<Engagement> mockPastEngagements() {

        // e8 - start date before r start date and end date before r start date and
        // launched and archive date before r start date

        Engagement e11 = MockUtils.mockMinimumEngagement("customer11", "project11", "11");
        // before range start date
        String startDate = LocalDate.parse(RANGE_START).minusMonths(4).toString();
        e11.setStartDate(startDate);
        // before range start date
        String endDate = LocalDate.parse(RANGE_START).minusMonths(3).toString();
        e11.setEndDate(endDate);
        // archive date 30 days after end date
        String archiveDate = LocalDate.parse(endDate).plusDays(30).toString();
        e11.setArchiveDate(archiveDate);
        e11.setLaunch(MockUtils.mockLaunch(startDate, "someone", "someone@example.com"));

        return Arrays.asList(e11);

    }

    private List<Engagement> mockTerminatingEngagements() {

        // e6 - start date before r start date and end date before r start date and
        // launched and archive date after r start date

        Engagement e12 = MockUtils.mockMinimumEngagement("customer12", "project12", "12");
        // before range start date
        String startDate = LocalDate.parse(RANGE_START).minusMonths(4).toString();
        e12.setStartDate(startDate);
        // before range start date
        String endDate = LocalDate.parse(RANGE_START).minusDays(10).toString();
        e12.setEndDate(endDate);
        // archive date 30 days after end date
        String archiveDate = LocalDate.parse(endDate).plusDays(30).toString();
        e12.setArchiveDate(archiveDate);
        e12.setLaunch(MockUtils.mockLaunch(startDate, "someone", "someone@example.com"));

        return Arrays.asList(e12);

    }

}