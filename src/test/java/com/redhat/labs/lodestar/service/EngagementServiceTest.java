package com.redhat.labs.lodestar.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.PropertyNamingStrategy;
import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.redhat.labs.lodestar.model.Artifact;
import com.redhat.labs.lodestar.model.Category;
import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.EngagementUser;
import com.redhat.labs.lodestar.model.Hook;
import com.redhat.labs.lodestar.model.HostingEnvironment;
import com.redhat.labs.lodestar.model.Launch;
import com.redhat.labs.lodestar.model.Score;
import com.redhat.labs.lodestar.model.UseCase;
import com.redhat.labs.lodestar.model.event.EventType;
import com.redhat.labs.lodestar.model.filter.FilterOptions;
import com.redhat.labs.lodestar.model.filter.ListFilterOptions;
import com.redhat.labs.lodestar.model.pagination.PagedStringResults;
import com.redhat.labs.lodestar.repository.EngagementRepository;
import com.redhat.labs.lodestar.rest.client.LodeStarGitLabAPIService;
import com.redhat.labs.lodestar.utils.MockUtils;

import io.vertx.mutiny.core.eventbus.EventBus;

class EngagementServiceTest {

    JsonbConfig config = new JsonbConfig().withFormatting(true)
            .withPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CASE_WITH_UNDERSCORES);
    Jsonb jsonb = JsonbBuilder.create(config);

    EngagementRepository repository;
    EventBus eventBus;
    LodeStarGitLabAPIService gitApi;

    EngagementService service;

    @BeforeEach
    void setup() {

        repository = Mockito.mock(EngagementRepository.class);
        eventBus = Mockito.mock(EventBus.class);
        gitApi = Mockito.mock(LodeStarGitLabAPIService.class);

        service = new EngagementService();
        service.statusFile = "status.json";
        service.commitFilteredMessages = Lists.newArrayList("manual_refresh");
        service.jsonb = jsonb;
        service.repository = repository;
        service.eventBus = eventBus;
        service.gitApi = gitApi;

    }

    @AfterEach
    void tearDown() {
        Mockito.reset(repository, eventBus, gitApi);
    }

    // create

    @ParameterizedTest
    @ValueSource(strings = { "customer", "project" })
    void testCreateNamesTooLong(String name) {

        Engagement e = MockUtils.mockMinimumEngagement("c1", "p1", "1234");
        String value = StringUtils.repeat("a", 256);

        if ("customer".equals(name)) {
            e.setCustomerName(value);
        } else {
            e.setProjectName(value);
        }

        WebApplicationException wae = assertThrows(WebApplicationException.class, () -> service.create(e));
        assertEquals(400, wae.getResponse().getStatus());
        assertEquals("names cannot be greater than 255 characters.", wae.getMessage());

    }

    @Test
    void testCreateAlreadyExists() {

        Engagement e = MockUtils.mockMinimumEngagement("c1", "p1", "1234");
        Mockito.when(repository.findByUuid("1234")).thenReturn(Optional.of(e));

        WebApplicationException wae = assertThrows(WebApplicationException.class, () -> service.create(e));
        assertEquals(409, wae.getResponse().getStatus());
        assertEquals("engagement already exists, use PUT to update resource", wae.getMessage());

    }

    @Test
    void testCreateDuplicateSubdomainInHostingEnvironments() {

        Engagement e = MockUtils.mockMinimumEngagement("c1", "p1", "1234");
        HostingEnvironment he1 = MockUtils.mockHostingEnvironment("env1", "subdomain");
        HostingEnvironment he2 = MockUtils.mockHostingEnvironment("env2", "subdomain");
        e.setHostingEnvironments(Lists.newArrayList(he1, he2));

        Mockito.when(repository.findByUuid("1234")).thenReturn(Optional.empty());

        WebApplicationException wae = assertThrows(WebApplicationException.class, () -> service.create(e));
        assertEquals(400, wae.getResponse().getStatus());
        assertEquals("supplied hosting environments has duplicate subdomains for entries [subdomain]",
                wae.getMessage());

    }

    @Test
    void testCreateHostingEnvironmentSubdomainAlreadyUsed() {

        Engagement e = MockUtils.mockMinimumEngagement("c1", "p1", "1234");
        HostingEnvironment he1 = MockUtils.mockHostingEnvironment("env1", "subdomain1");
        HostingEnvironment he2 = MockUtils.mockHostingEnvironment("env2", "subdomain2");
        e.setHostingEnvironments(Lists.newArrayList(he1, he2));

        Mockito.when(repository.findByUuid("1234")).thenReturn(Optional.empty());
        Mockito.when(repository.findBySubdomain(Mockito.anyString())).thenReturn(Optional.of(e));

        WebApplicationException wae = assertThrows(WebApplicationException.class, () -> service.create(e));
        assertEquals(409, wae.getResponse().getStatus());
        assertEquals("The following subdomains are already in use: [subdomain1, subdomain2]", wae.getMessage());

    }

    @Test
    void testCreateSuccess() {

        Engagement e = MockUtils.mockMinimumEngagement("c1", "p1", "1234");

        Mockito.when(repository.findByUuid("1234")).thenReturn(Optional.empty());

        Engagement created = service.create(e);
        assertNotNull(created);
        assertNotNull(created.getUuid());
        assertNotNull(created.getLastUpdate());
        assertNotNull(created.getCreationDetails());
        assertNull(created.getCommitMessage());

        Mockito.verify(repository, Mockito.times(1)).persist(e);
        Mockito.verify(eventBus, Mockito.times(1)).sendAndForget(Mockito.eq(EventType.CREATE_ENGAGEMENT_EVENT_ADDRESS),
                Mockito.any());

    }

    // update

    @ParameterizedTest
    @ValueSource(strings = { "customer", "project" })
    void testUpdateNamesTooLong(String name) {

        String value = StringUtils.repeat("a", 256);

        Engagement toUpdate = MockUtils.mockMinimumEngagement("c3", "p3", "1234");
        toUpdate.setProjectId(1111);
        EngagementUser user1 = MockUtils.mockEngagementUser("b.s@example.com", "bill", "smith", "dev", null, true);
        toUpdate.setEngagementUsers(Sets.newHashSet(user1));

        if ("customer".equals(name)) {
            toUpdate.setCustomerName(value);
        } else {
            toUpdate.setProjectName(value);
        }

        Engagement persisted = MockUtils.mockMinimumEngagement("c1", "p1", "1234");
        persisted.setProjectId(1111);

        Mockito.when(repository.findByUuid("1234")).thenReturn(Optional.of(persisted));

        WebApplicationException wae = assertThrows(WebApplicationException.class, () -> service.create(toUpdate));
        assertEquals(400, wae.getResponse().getStatus());
        assertEquals("names cannot be greater than 255 characters.", wae.getMessage());

    }

    @Test
    void testUpdateDoesNotExist() {

        Engagement e = MockUtils.mockMinimumEngagement("c1", "p1", "1234");
        Mockito.when(repository.findByUuid("1234")).thenReturn(Optional.empty());

        WebApplicationException wae = assertThrows(WebApplicationException.class, () -> service.update(e));
        assertEquals(404, wae.getResponse().getStatus());
        assertEquals("no engagement found, use POST to create", wae.getMessage());

    }

    @Test
    void testUpdateDuplicateSubdomainInHostingEnvironments() {

        Engagement e = MockUtils.mockMinimumEngagement("c1", "p1", "1234");
        e.setProjectId(1111);
        HostingEnvironment he1 = MockUtils.mockHostingEnvironment("env1", "subdomain");
        HostingEnvironment he2 = MockUtils.mockHostingEnvironment("env2", "subdomain");
        e.setHostingEnvironments(Lists.newArrayList(he1, he2));

        Mockito.when(repository.findByUuid("1234")).thenReturn(Optional.of(e));

        WebApplicationException wae = assertThrows(WebApplicationException.class, () -> service.update(e));
        assertEquals(400, wae.getResponse().getStatus());
        assertEquals("supplied hosting environments has duplicate subdomains for entries [subdomain]",
                wae.getMessage());

    }

    @Test
    void testUpdateSubdomainExistsAndNotCurrentEngagement() throws Exception {

        Engagement e = MockUtils.mockMinimumEngagement("c1", "p1", "1234");
        e.setProjectId(1111);
        HostingEnvironment he1 = MockUtils.mockHostingEnvironment("env1", "subdomain1");
        HostingEnvironment he2 = MockUtils.mockHostingEnvironment("env2", "subdomain2");
        e.setHostingEnvironments(Lists.newArrayList(he1, he2));
        Engagement e2 = MockUtils.cloneEngagement(e);
        e2.setProjectId(2222);

        Mockito.when(repository.findByUuid("1234")).thenReturn(Optional.of(e));
        Mockito.when(repository.findBySubdomain(Mockito.anyString(), Mockito.any())).thenReturn(Optional.empty());
        Mockito.when(repository.findBySubdomain(Mockito.anyString())).thenReturn(Optional.of(e2));

        WebApplicationException wae = assertThrows(WebApplicationException.class, () -> service.update(e));
        assertEquals(409, wae.getResponse().getStatus());
        assertEquals("The following subdomains are already in use: [subdomain1, subdomain2]", wae.getMessage());

    }

    @Test
    void testUpdateNameChangedConflictExists() throws Exception {

        Engagement toUpdate = MockUtils.mockMinimumEngagement("c3", "p3", "1234");
        toUpdate.setProjectId(1111);

        Engagement persisted = MockUtils.cloneEngagement(toUpdate);
        persisted.setCustomerName("c1");
        persisted.setProjectName("p1");

        Engagement other = MockUtils.mockMinimumEngagement("c3", "p3", "4545");
        other.setProjectId(2222);

        Mockito.when(repository.findByUuid("1234")).thenReturn(Optional.of(persisted));
        Mockito.when(repository.findByCustomerNameAndProjectName("c3", "p3", new FilterOptions()))
                .thenReturn(Optional.of(other));

        WebApplicationException wae = assertThrows(WebApplicationException.class, () -> service.update(toUpdate));
        assertEquals(409, wae.getResponse().getStatus());
        assertEquals("failed to change name(s).  engagement with customer name 'c3' and project 'p3' already exists.",
                wae.getMessage());

    }

    @Test
    void testUpdateSuccessNameChange() {

        Engagement toUpdate = MockUtils.mockMinimumEngagement("c3", "p3", "1234");
        toUpdate.setProjectId(1111);
        EngagementUser user1 = MockUtils.mockEngagementUser("b.s@example.com", "bill", "smith", "dev", null, true);
        toUpdate.setEngagementUsers(Sets.newHashSet(user1));

        Engagement persisted = MockUtils.mockMinimumEngagement("c1", "p1", "1234");
        persisted.setProjectId(1111);

        Mockito.when(repository.findByUuid("1234")).thenReturn(Optional.of(persisted));
        Mockito.when(repository.findByCustomerNameAndProjectName("c3", "p3", new FilterOptions()))
                .thenReturn(Optional.empty());
        Mockito.when(repository.updateEngagementIfLastUpdateMatched(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(toUpdate));

        Engagement updated = service.update(toUpdate);
        assertNotNull(updated);
        assertNotNull(updated.getLastUpdate());
        assertNotNull(updated.getEngagementUsers());

        Set<EngagementUser> users = updated.getEngagementUsers();
        assertEquals(1, updated.getEngagementUsers().size());

        EngagementUser user = users.iterator().next();
        assertNotNull(user.getUuid());
        assertFalse(user.isReset());

        Mockito.verify(eventBus, Mockito.times(1)).sendAndForget(Mockito.eq(EventType.UPDATE_ENGAGEMENT_EVENT_ADDRESS),
                Mockito.any());

    }

    @Test
    void testUpdateSuccessNoNameChange() throws Exception {

        Engagement toUpdate = MockUtils.mockMinimumEngagement("c1", "p1", "1234");
        toUpdate.setProjectId(1111);
        EngagementUser user1 = MockUtils.mockEngagementUser("b.s@example.com", "bill", "smith", "dev", "4444", true);
        toUpdate.setEngagementUsers(Sets.newHashSet(user1));

        Engagement persisted = MockUtils.cloneEngagement(toUpdate);

        Mockito.when(repository.findByUuid("1234")).thenReturn(Optional.of(persisted));
        Mockito.when(repository.findByCustomerNameAndProjectName("c3", "p3", new FilterOptions()))
                .thenReturn(Optional.empty());
        Mockito.when(repository.updateEngagementIfLastUpdateMatched(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(toUpdate));

        Engagement updated = service.update(toUpdate);
        assertNotNull(updated);
        assertNotNull(updated.getLastUpdate());
        assertNotNull(updated.getEngagementUsers());

        Set<EngagementUser> users = updated.getEngagementUsers();
        assertEquals(1, updated.getEngagementUsers().size());

        EngagementUser user = users.iterator().next();
        assertNotNull(user.getUuid());
        assertEquals("4444", user.getUuid());
        assertFalse(user.isReset());

        Mockito.verify(eventBus, Mockito.times(1)).sendAndForget(Mockito.eq(EventType.UPDATE_ENGAGEMENT_EVENT_ADDRESS),
                Mockito.any());

    }

    // getBySubdomain

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "  ", "unknown" })
    void testGetBySubdomainBlankAndNotFound(String subdomain) {
        assertTrue(service.getBySubdomain(subdomain).isEmpty());
    }

    @Test
    void testGetBySubdomainFound() {
        Mockito.when(repository.findBySubdomain("subdomain"))
                .thenReturn(Optional.of(MockUtils.mockMinimumEngagement("c1", "p1", "1234")));
        assertTrue(service.getBySubdomain("subdomain").isPresent());
    }

    // setProjectId

    @Test
    void testSetProjectId() {

        service.setProjectId("1234", 4444);
        Mockito.verify(repository, Mockito.times(1)).setProjectId("1234", 4444);

    }

    // updateStatusAndCommits

    @Test
    void testUpdateStatusAndCommitsEngagementNotFound() {

        Hook hook = MockUtils.mockHook("/nope/nada/iac", "/ nope/ nada/ iac", false, "status.json");
        Mockito.when(gitApi.getEngagementByNamespace("/nope/nada/iac"))
                .thenReturn(MockUtils.mockMinimumEngagement("nope", "nada", null));

        WebApplicationException wae = assertThrows(WebApplicationException.class,
                () -> service.updateStatusAndCommits(hook));
        assertEquals(404, wae.getResponse().getStatus());
        assertEquals("no engagement found. unable to update from hook.", wae.getMessage());

    }

    @Test
    void testUpdateStatusAndCommitsSuccessFileDidNotChange() {

        Hook hook = MockUtils.mockHook("/nope/nada/iac", "/ nope / nada / iac", false, "status.json");

        Mockito.when(repository.findByCustomerNameAndProjectName("nope", "nada"))
                .thenReturn(Optional.of(MockUtils.mockMinimumEngagement("nope", "nada", null)));

        Mockito.when(gitApi.getCommits("nope", "nada"))
                .thenReturn(Lists.newArrayList(MockUtils.mockCommit("status.json", false, "msg")));

        service.updateStatusAndCommits(hook);

        Mockito.verify(eventBus).sendAndForget(Mockito.eq(EventType.UPDATE_COMMITS_EVENT_ADDRESS),
                Mockito.any(Engagement.class));
        Mockito.verify(eventBus, Mockito.times(0)).sendAndForget(Mockito.eq(EventType.UPDATE_STATUS_EVENT_ADDRESS),
                Mockito.any(Engagement.class));

    }

    @Test
    void testUpdateStatusAndCommitsSuccessFileDidChange() {

        Hook hook = MockUtils.mockHook("/nope/nada/iac", "/ nope / nada / iac", true, "status.json");

        Mockito.when(repository.findByCustomerNameAndProjectName("nope", "nada"))
                .thenReturn(Optional.of(MockUtils.mockMinimumEngagement("nope", "nada", null)));

        Mockito.when(gitApi.getStatus("nope", "nada")).thenReturn(MockUtils.mockStatus("green"));
        Mockito.when(gitApi.getCommits("nope", "nada"))
                .thenReturn(Lists.newArrayList(MockUtils.mockCommit("status.json", false, "msg")));

        service.updateStatusAndCommits(hook);

        Mockito.verify(eventBus).sendAndForget(Mockito.eq(EventType.UPDATE_COMMITS_EVENT_ADDRESS),
                Mockito.any(Engagement.class));
        Mockito.verify(eventBus).sendAndForget(Mockito.eq(EventType.UPDATE_STATUS_EVENT_ADDRESS),
                Mockito.any(Engagement.class));

    }

    @Test
    void testUpdateStatusAndCommitsSuccessRefreshEngagement() {

        Hook hook = MockUtils.mockHook("/nope/nada/iac", "/ nope / nada / iac", true, "status.json", "manual_refresh");
        hook.setProjectId(1234);

        service.commitFilteredMessages = Lists.newArrayList("manual_refresh");
        service.updateStatusAndCommits(hook);

        Mockito.verify(eventBus).sendAndForget(EventType.DELETE_AND_RELOAD_ENGAGEMENT_EVENT_ADDRESS, "1234");
        Mockito.verify(eventBus, Mockito.times(0)).sendAndForget(Mockito.eq(EventType.UPDATE_COMMITS_EVENT_ADDRESS),
                Mockito.any(Engagement.class));
        Mockito.verify(eventBus, Mockito.times(0)).sendAndForget(Mockito.eq(EventType.UPDATE_STATUS_EVENT_ADDRESS),
                Mockito.any(Engagement.class));

    }

    // getByCustomerAndProjectName

    @Test
    void testGetByCustomerNameAndProjectNameNotFound() {

        Mockito.when(
                repository.findByCustomerNameAndProjectName(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
                .thenReturn(Optional.empty());

        WebApplicationException wae = assertThrows(WebApplicationException.class,
                () -> service.getByCustomerAndProjectName("c1", "p1", new FilterOptions()));
        assertEquals(404, wae.getResponse().getStatus());
        assertEquals("no engagement found with customer:project c1:p1", wae.getMessage());

    }

    @Test
    void testGetByCustomerNameAndProjectName() {

        Mockito.when(
                repository.findByCustomerNameAndProjectName(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
                .thenReturn(Optional.of(MockUtils.mockMinimumEngagement("c1", "p1", "1234")));

        Engagement e = service.getByCustomerAndProjectName("c1", "p1", new FilterOptions());
        assertNotNull(e);
        assertEquals("c1", e.getCustomerName());
        assertEquals("p1", e.getProjectName());
        assertEquals("1234", e.getUuid());

    }

    // getByUuid

    @Test
    void testGetByUuidNotFound() {

        Mockito.when(repository.findByUuid("1234")).thenReturn(Optional.empty());

        WebApplicationException wae = assertThrows(WebApplicationException.class,
                () -> service.getByUuid("1234", new FilterOptions()));
        assertEquals(404, wae.getResponse().getStatus());
        assertEquals("no engagement found with id 1234", wae.getMessage());

    }

    @Test
    void testGetByUuid() {

        Mockito.when(repository.findByUuid("1234", new FilterOptions()))
                .thenReturn(Optional.of(MockUtils.mockMinimumEngagement("c1", "p1", "1234")));

        Engagement e = service.getByUuid("1234", new FilterOptions());
        assertNotNull(e);
        assertEquals("c1", e.getCustomerName());
        assertEquals("p1", e.getProjectName());
        assertEquals("1234", e.getUuid());

    }

    // getAll

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "  " })
    void testGetAllNoCategorySpecified(String categories) {

        service.getEngagementsPaged(new ListFilterOptions());

        Mockito.verify(repository).findPagedEngagements(new ListFilterOptions());
        Mockito.verify(repository, Mockito.times(0)).findCategories(new ListFilterOptions());

    }

    @Test
    void testGetAllWithCategories() {

        ListFilterOptions options = new ListFilterOptions();
        options.setSuggestFieldName(Optional.of("hello"));
        service.getEngagementsPaged(options);

        Mockito.verify(repository).findPagedEngagements(options);

    }

    // getSuggestions

    @Test
    void testGetSuggestionsNoneFound() {

        ListFilterOptions options = new ListFilterOptions();
        options.setSearch("customer_name like c");
        PagedStringResults results = PagedStringResults.builder().results(Lists.newArrayList()).build();
        Mockito.when(repository.findCustomerSuggestions(options)).thenReturn(results);

        PagedStringResults pagedResults = service.getSuggestions(options);
        Collection<String> suggestions = pagedResults.getResults();
        assertNotNull(suggestions);
        assertTrue(suggestions.isEmpty());

    }

    @Test
    void testGetSuggestions() {

        Engagement e1 = MockUtils.mockMinimumEngagement("customer1", "project1", "1234");
        Engagement e2 = MockUtils.mockMinimumEngagement("customer2", "project1", "5454");
        ListFilterOptions options = new ListFilterOptions();
        PagedStringResults results = PagedStringResults.builder()
                .results(Lists.newArrayList(e1.getCustomerName(), e2.getCustomerName())).build();
        options.setSearch("customer_name like c");
        Mockito.when(repository.findCustomerSuggestions(options)).thenReturn(results);

        PagedStringResults pagedResults = service.getSuggestions(options);
        List<String> suggestions = pagedResults.getResults();
        assertNotNull(suggestions);
        assertEquals(2, suggestions.size());
        assertTrue(suggestions.contains("customer1"));
        assertTrue(suggestions.contains("customer2"));

    }

    // deleteByCustomerAndProjectName

    @Test
    void testDeleteByCustomerAndProjectNameNotFound() {

        Mockito.when(repository.findByCustomerNameAndProjectName("c1", "p1", new FilterOptions()))
                .thenReturn(Optional.empty());

        WebApplicationException wae = assertThrows(WebApplicationException.class,
                () -> service.deleteByCustomerAndProjectName("c1", "p1"));
        assertEquals(404, wae.getResponse().getStatus());
        assertEquals("no engagement found with customer:project c1:p1", wae.getMessage());

    }

    @Test
    void testDeleteByCustomerAndProjectName() {

        Engagement e = MockUtils.mockMinimumEngagement("c1", "p1", "1234");
        Mockito.when(repository.findByCustomerNameAndProjectName("c1", "p1", new FilterOptions()))
                .thenReturn(Optional.of(e));

        service.deleteByCustomerAndProjectName("c1", "p1");

        Mockito.verify(repository).delete(e);

    }

    // deleteByUuid

    @Test
    void testDeleteByUuidNotFound() {

        Mockito.when(repository.findByUuid("1234")).thenReturn(Optional.empty());

        WebApplicationException wae = assertThrows(WebApplicationException.class, () -> service.deleteByUuid("1234"));
        assertEquals(404, wae.getResponse().getStatus());
        assertEquals("no engagement found with id 1234", wae.getMessage());

    }

    @Test
    void testDeleteByUuid() {

        Engagement e = MockUtils.mockMinimumEngagement("c1", "p1", "1234");
        Mockito.when(repository.findByUuid("1234", new FilterOptions())).thenReturn(Optional.of(e));

        service.deleteByUuid("1234");

        Mockito.verify(repository).delete(e);

    }

    // deleteEngagement

    @Test
    void testDeleteEngagementNotFound() {

        Mockito.when(repository.findByUuid("1234")).thenReturn(Optional.empty());

        WebApplicationException wae = assertThrows(WebApplicationException.class,
                () -> service.deleteEngagement("1234"));
        assertEquals(404, wae.getResponse().getStatus());
        assertEquals("no engagement found with id 1234", wae.getMessage());

    }

    @Test
    void testDeleteEngagementAlreadyLaunched() {

        Engagement e = MockUtils.mockMinimumEngagement("c1", "p1", "1234");
        e.setLaunch(Launch.builder().build());
        Mockito.when(repository.findByUuid("1234", new FilterOptions())).thenReturn(Optional.of(e));

        WebApplicationException wae = assertThrows(WebApplicationException.class,
                () -> service.deleteEngagement("1234"));
        assertEquals(400, wae.getResponse().getStatus());
        assertEquals("cannot delete engagement that has already been launched.", wae.getMessage());

    }

    @Test
    void testDeleteEngagement() {

        Engagement e = MockUtils.mockMinimumEngagement("c1", "p1", "1234");
        Mockito.when(repository.findByUuid("1234", new FilterOptions())).thenReturn(Optional.of(e));

        service.deleteEngagement("1234");

        Mockito.verify(repository).delete(e);
        Mockito.verify(eventBus).sendAndForget(EventType.DELETE_ENGAGEMENT_EVENT_ADDRESS, e);

    }

    // getCategories

    @Test
    void testGetCategories() {

        ListFilterOptions options = new ListFilterOptions();
        service.getCategories(options);

        Mockito.verify(repository).findCategories(options);

    }

    // getArtifactTypes

    @Test
    void testGetArtifactTypes() {

        ListFilterOptions options = new ListFilterOptions();
        service.getArtifactTypes(options);

        Mockito.verify(repository).findArtifactTypes(options);

    }

    // setNullUuids

    @Test
    void testSetNullUuidsAlreadySet() {

        UseCase u1 = MockUtils.mockUseCase("case 1", "case one", 0);
        u1.setUuid(UUID.randomUUID().toString());
        u1.setCreatedAndUpdated();
        UseCase u2 = MockUtils.mockUseCase("case 2", "case two", 1);
        Category c1 = MockUtils.mockCategory("cat1");
        c1.setUuid(UUID.randomUUID().toString());
        c1.setCreatedAndUpdated();
        Category c2 = MockUtils.mockCategory("cat2");
        Artifact a1 = MockUtils.mockArtifact("demo 1", "demo", "http://demo-1");
        a1.setUuid(UUID.randomUUID().toString());
        a1.setCreatedAndUpdated();
        Artifact a2 = MockUtils.mockArtifact("demo 2", "demo", "http://demo-2");
        EngagementUser user1 = MockUtils.mockEngagementUser("b.s@example.com", "bill", "smith", "dev", "1234", false);
        EngagementUser user2 = MockUtils.mockEngagementUser("t.j@example.com", "tom", "jones", "dev", "4321", false);
        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "p2", null);
        e1.setEngagementUsers(Sets.newHashSet(user1, user2));
        e1.setArtifacts(Arrays.asList(a1, a2));
        e1.setCategories(Arrays.asList(c1, c2));
        e1.setUseCases(Arrays.asList(u1, u2));

        HostingEnvironment he1 = MockUtils.mockHostingEnvironment("env1", "env1");
        he1.setUuid(UUID.randomUUID().toString());
        he1.setCreatedAndUpdated();
        HostingEnvironment he2 = MockUtils.mockHostingEnvironment("env2", "env2");
        Score s1 = MockUtils.mockScore("score1", 90.9);
        s1.setUuid(UUID.randomUUID().toString());
        s1.setCreatedAndUpdated();
        Score s2 = MockUtils.mockScore("score2", 55.4);
        EngagementUser user3 = MockUtils.mockEngagementUser("j.s@example.com", "jim", "smith", "dev", null, false);
        EngagementUser user4 = MockUtils.mockEngagementUser("t.j@example.com", "tom", "jones", "dev", "4321", false);
        Engagement e2 = MockUtils.mockMinimumEngagement("c1", "p2", "7890");
        e2.setEngagementUsers(Sets.newHashSet(user3, user4));
        e2.setScores(Arrays.asList(s1, s2));
        e2.setHostingEnvironments(Arrays.asList(he1, he2));

        Mockito.when(repository.streamAll()).thenReturn(Stream.of(e1, e2));

        long count = service.setNullUuids();
        assertEquals(2, count);

        Mockito.verify(eventBus, Mockito.times(2)).sendAndForget(Mockito.eq(EventType.UPDATE_ENGAGEMENT_EVENT_ADDRESS),
                Mockito.any());
        Mockito.verify(repository).update(Mockito.anyIterable());

    }

    // syncGitToDatabase

    @Test
    void testSyncGitToDatabasePurgeFirst() {

        service.syncGitToDatabase(true, null, null);

        Mockito.verify(eventBus).sendAndForget(EventType.DELETE_AND_RELOAD_DATABASE_EVENT_ADDRESS,
                EventType.DELETE_AND_RELOAD_DATABASE_EVENT_ADDRESS);
        Mockito.verify(eventBus, Mockito.times(0)).sendAndForget(EventType.LOAD_DATABASE_EVENT_ADDRESS,
                EventType.LOAD_DATABASE_EVENT_ADDRESS);

    }

    @Test
    void testSyncGitToDatabaseDoNotPurgeFirst() {

        service.syncGitToDatabase(false, null, null);

        Mockito.verify(eventBus, Mockito.times(0)).sendAndForget(EventType.DELETE_AND_RELOAD_DATABASE_EVENT_ADDRESS,
                EventType.DELETE_AND_RELOAD_DATABASE_EVENT_ADDRESS);
        Mockito.verify(eventBus).sendAndForget(EventType.LOAD_DATABASE_EVENT_ADDRESS,
                EventType.LOAD_DATABASE_EVENT_ADDRESS);

    }

    @Test
    void testSyncGitToDatabaseWithProjectId() {

        service.syncGitToDatabase(false, null, "1234");

        Mockito.verify(eventBus).sendAndForget(EventType.DELETE_AND_RELOAD_ENGAGEMENT_EVENT_ADDRESS, "1234");
        Mockito.verify(eventBus, Mockito.times(0)).sendAndForget(EventType.DELETE_AND_RELOAD_DATABASE_EVENT_ADDRESS,
                EventType.DELETE_AND_RELOAD_DATABASE_EVENT_ADDRESS);
        Mockito.verify(eventBus, Mockito.times(0)).sendAndForget(EventType.LOAD_DATABASE_EVENT_ADDRESS,
                EventType.LOAD_DATABASE_EVENT_ADDRESS);

    }

    @Test
    void testSyncGitToDatabaseWithUuidFound() {

        Engagement e = MockUtils.mockMinimumEngagement("c1", "p1", "1234");
        Mockito.when(repository.findByUuid("1234", new FilterOptions())).thenReturn(Optional.of(e));

        service.syncGitToDatabase(false, "1234", null);

        Mockito.verify(eventBus).sendAndForget(EventType.DELETE_AND_RELOAD_ENGAGEMENT_EVENT_ADDRESS,
                String.valueOf(e.getProjectId()));
        Mockito.verify(eventBus, Mockito.times(0)).sendAndForget(EventType.DELETE_AND_RELOAD_DATABASE_EVENT_ADDRESS,
                EventType.DELETE_AND_RELOAD_DATABASE_EVENT_ADDRESS);
        Mockito.verify(eventBus, Mockito.times(0)).sendAndForget(EventType.LOAD_DATABASE_EVENT_ADDRESS,
                EventType.LOAD_DATABASE_EVENT_ADDRESS);

    }

    @Test
    void testSyncGitToDatabaseWithUuidNotFound() {

        Engagement e = MockUtils.mockMinimumEngagement("c1", "p1", "1234");
        Mockito.when(repository.findByUuid("1234", new FilterOptions())).thenReturn(Optional.empty());

        WebApplicationException wae = assertThrows(WebApplicationException.class,
                () -> service.syncGitToDatabase(false, "1234", null));
        assertEquals(404, wae.getResponse().getStatus());
        assertEquals("no engagement found with id 1234", wae.getMessage());

        Mockito.verify(eventBus, Mockito.times(0)).sendAndForget(EventType.DELETE_AND_RELOAD_ENGAGEMENT_EVENT_ADDRESS,
                e);
        Mockito.verify(eventBus, Mockito.times(0)).sendAndForget(EventType.DELETE_AND_RELOAD_DATABASE_EVENT_ADDRESS,
                EventType.DELETE_AND_RELOAD_DATABASE_EVENT_ADDRESS);
        Mockito.verify(eventBus, Mockito.times(0)).sendAndForget(EventType.LOAD_DATABASE_EVENT_ADDRESS,
                EventType.LOAD_DATABASE_EVENT_ADDRESS);

    }

    // launch

    @Test
    void testLaunchAlreadyLaunced() {

        Engagement e = MockUtils.mockMinimumEngagement("c1", "p1", "1234");
        e.setLaunch(Launch.builder().build());

        WebApplicationException wae = assertThrows(WebApplicationException.class, () -> service.launch(e));
        assertEquals(400, wae.getResponse().getStatus());
        assertEquals("engagement has already been launched.", wae.getMessage());

    }

    @Test
    void testLaunch() {

        Engagement toUpdate = MockUtils.mockMinimumEngagement("c1", "p1", "1234");
        toUpdate.setProjectId(1111);

        Engagement persisted = MockUtils.mockMinimumEngagement("c1", "p1", "1234");
        persisted.setProjectId(1111);

        Mockito.when(repository.findByUuid("1234")).thenReturn(Optional.of(persisted));
        Mockito.when(repository.updateEngagementIfLastUpdateMatched(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(toUpdate));

        Engagement updated = service.launch(toUpdate);
        assertNotNull(updated);
        assertNotNull(updated.getLaunch());

        Mockito.verify(repository).updateEngagementIfLastUpdateMatched(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(eventBus).sendAndForget(Mockito.eq(EventType.UPDATE_ENGAGEMENT_EVENT_ADDRESS), Mockito.any());

    }

    @Test
    void testPersistEngagementIfNotFound() {

        Engagement e = MockUtils.mockMinimumEngagement("c1", "p1", "1234");

        Mockito.when(repository.findByUuid("1234")).thenReturn(Optional.empty());

        assertTrue(service.persistEngagementIfNotFound(e));

        Mockito.verify(repository).persist(Mockito.any(Engagement.class));

    }

    @Test
    void testPersistEngagementIfNotFoundFound() {

        Engagement e = MockUtils.mockMinimumEngagement("c1", "p1", "1234");

        Mockito.when(repository.findByUuid("1234")).thenReturn(Optional.of(e));

        assertFalse(service.persistEngagementIfNotFound(e));

        Mockito.verify(repository, Mockito.times(0)).persist(Mockito.any(Engagement.class));

    }

    @Test
    void testSetEngagementAttributeIdsAndTimestamps() {

        Engagement persisted = MockUtils.mockMinimumEngagement("c1", "p1", "1");

        // will be removed
        HostingEnvironment he = MockUtils.mockHostingEnvironment("env", "env");
        persisted.setHostingEnvironments(Arrays.asList(he));

        // will be updated
        Artifact a1 = MockUtils.mockArtifact("report 1", "report", "http://report-1");
        String a1Id = UUID.randomUUID().toString();
        a1.setUuid(a1Id);
        a1.setCreatedAndUpdated();

        // will not be updated
        Artifact a2 = MockUtils.mockArtifact("whitepaper 1", "whitepaper", "http://whitepaper-1");
        String a2Id = UUID.randomUUID().toString();
        a2.setUuid(a2Id);
        a2.setCreatedAndUpdated();

        persisted.setArtifacts(Arrays.asList(a1, a2));

        String updatedJson = jsonb.toJson(persisted);
        Engagement updated = jsonb.fromJson(updatedJson, Engagement.class);

        // add category
        Category category = MockUtils.mockCategory("category1");
        updated.setCategories(Arrays.asList(category));

        // update one artifact
        updated.getArtifacts().get(0).setTitle("updated title");

        // add new artifact
        Artifact a3 = MockUtils.mockArtifact("demo 1", "demo", "http://demo-1");
        updated.getArtifacts().add(a3);

        // remove hosting environment
        updated.setHostingEnvironments(Arrays.asList());

        // when
        service.setIdsAndTimestamps(updated, persisted);

        // hosting environment removed
        assertTrue(updated.getHostingEnvironments().isEmpty());

        // category should be added
        assertEquals(1, updated.getCategories().size());
        Category c = updated.getCategories().get(0);
        assertNotNull(c.getUuid());
        assertNotNull(c.getCreated());
        assertNotNull(c.getUpdated());
        assertEquals("category1", c.getName());

        // a1 updated, a2 unchanged, a3 added
        assertEquals(3, updated.getArtifacts().size());

        updated.getArtifacts().stream().forEach(a -> {

            assertNotNull(c.getUuid());

            if (a.getUuid().equals(a1.getUuid())) {

                // a1 created unchanged, modified changed
                assertEquals(a1.getCreated(), a.getCreated());
                assertNotEquals(a1.getUpdated(), a.getUpdated());

            } else if (a.getUuid().equals(a2.getUuid())) {

                // a2 created unchanged, modified unchanged
                assertEquals(a2.getCreated(), a.getCreated());
                assertEquals(a2.getUpdated(), a.getUpdated());

            } else {

                // a3 created and modified set
                assertNotNull(c.getCreated());
                assertNotNull(c.getUpdated());

            }

        });

    }

}