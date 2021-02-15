package com.redhat.labs.lodestar.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;
import com.redhat.labs.lodestar.model.Artifact;
import com.redhat.labs.lodestar.model.Category;
import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.FilterOptions;
import com.redhat.labs.lodestar.model.HostingEnvironment;
import com.redhat.labs.lodestar.utils.EmbeddedMongoTest;
import com.redhat.labs.lodestar.utils.MockUtils;

import io.quarkus.test.junit.QuarkusTest;

@EmbeddedMongoTest
@QuarkusTest
class EngagementRepositoryTest {

    @Inject
    EngagementRepository repository;

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
        ;
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
        ;
        assertEquals(e.getHostingEnvironments(), persisted.getHostingEnvironments());

    }

    @Test
    void testFindCustomerSuggestions() {

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "c2", "1234");
        Engagement e2 = MockUtils.mockMinimumEngagement("e1", "c2", "1234");

        repository.persist(Lists.newArrayList(e1, e2));

        List<Engagement> results = repository.findCustomerSuggestions("C");
        assertEquals(1, results.size());

        results = repository.findCustomerSuggestions("c");
        assertEquals(1, results.size());

        results = repository.findCustomerSuggestions("e");
        assertEquals(1, results.size());

        results = repository.findCustomerSuggestions("1");
        assertEquals(2, results.size());

    }

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

        List<Category> results = repository.findCategorySuggestions("c");
        assertEquals(2, results.size());

        results = repository.findCategorySuggestions("c2");
        assertEquals(1, results.size());

        results = repository.findCategorySuggestions("E");
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

        List<Category> results = repository.findAllCategoryWithCounts();

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

        List<String> results = repository.findArtifactTypeSuggestions("de");
        assertEquals(2, results.size());
        assertTrue(results.contains("demo"));

        results = repository.findArtifactTypeSuggestions("V");
        assertEquals(1, results.size());
        assertTrue(results.contains("video"));

        results = repository.findArtifactTypeSuggestions("St");
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

        List<String> results = repository.findAllArtifactTypes();
        assertEquals(3, results.size());
        assertTrue(results.contains("demo"));
        assertTrue(results.contains("video"));
        assertTrue(results.contains("status"));

    }

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

        Optional<Engagement> optional = repository.findByUuid("1234", Optional.of(fo));
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

        Optional<Engagement> optional = repository.findByUuid("1234", Optional.of(fo));
        assertTrue(optional.isPresent());
        Engagement result = optional.get();
        assertNull(result.getUuid());
        assertNotNull(result.getCustomerName());
        assertNotNull(result.getProjectName());

    }

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

        Optional<Engagement> optional = repository.findByCustomerNameAndProjectName("c1", "c2", Optional.of(fo));
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

        Optional<Engagement> optional = repository.findByCustomerNameAndProjectName("c1", "c2", Optional.of(fo));
        assertTrue(optional.isPresent());
        Engagement result = optional.get();
        assertNull(result.getUuid());
        assertNotNull(result.getCustomerName());
        assertNotNull(result.getProjectName());

    }

    @Test
    void testFindAll() {

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "c2", "1234");
        repository.persist(e1);

        List<Engagement> results = repository.findAll(Optional.empty());
        assertEquals(1, results.size());

    }

    @Test
    void testFindWithInclude() {

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "c2", "1234");
        repository.persist(e1);

        FilterOptions fo = FilterOptions.builder().include("uuid").build();

        List<Engagement> results = repository.findAll(Optional.of(fo));
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

        FilterOptions fo = FilterOptions.builder().exclude("uuid").build();

        List<Engagement> results = repository.findAll(Optional.of(fo));
        assertEquals(1, results.size());

        Engagement result = results.get(0);
        assertNull(result.getUuid());
        assertNotNull(result.getCustomerName());
        assertNotNull(result.getProjectName());

    }

}