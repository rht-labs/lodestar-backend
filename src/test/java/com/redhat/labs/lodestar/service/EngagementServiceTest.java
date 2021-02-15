package com.redhat.labs.lodestar.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.PropertyNamingStrategy;
import javax.ws.rs.WebApplicationException;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.EngagementUser;
import com.redhat.labs.lodestar.model.Hook;
import com.redhat.labs.lodestar.model.HostingEnvironment;
import com.redhat.labs.lodestar.model.Launch;
import com.redhat.labs.lodestar.model.event.EventType;
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

    @Test
    void testUpdateDoesNotExist() {

        Engagement e = MockUtils.mockMinimumEngagement("c1", "p1", "1234");
        Mockito.when(repository.findByUuid("1234")).thenReturn(Optional.empty());

        WebApplicationException wae = assertThrows(WebApplicationException.class, () -> service.update(e));
        assertEquals(404, wae.getResponse().getStatus());
        assertEquals("no engagement found, use POST to create", wae.getMessage());

    }

    @Test
    void testUpdatePersistedHasNoProjectId() {

        Engagement e = MockUtils.mockMinimumEngagement("c1", "p1", "1234");
        Mockito.when(repository.findByUuid("1234")).thenReturn(Optional.of(e));

        WebApplicationException wae = assertThrows(WebApplicationException.class, () -> service.update(e));
        assertEquals(500, wae.getResponse().getStatus());
        assertEquals("cannot updated engagement: missing project id.", wae.getMessage());

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
        Mockito.when(repository.findByCustomerNameAndProjectName("c3", "p3", Optional.empty()))
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
        Mockito.when(repository.findByCustomerNameAndProjectName("c3", "p3", Optional.empty()))
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
        Mockito.when(repository.findByCustomerNameAndProjectName("c3", "p3", Optional.empty()))
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
                .thenReturn(Lists.newArrayList(MockUtils.mockCommit("status.json", false)));

        Engagement updated = service.updateStatusAndCommits(hook);

        assertNotNull(updated);
        assertNull(updated.getStatus());
        assertNotNull(updated.getCommits());
        assertEquals(1, updated.getCommits().size());

        Mockito.verify(gitApi, Mockito.times(0)).getStatus("nope", "nada");
        Mockito.verify(repository).update(updated);

    }

    @Test
    void testUpdateStatusAndCommitsSuccessFileDidChange() {

        Hook hook = MockUtils.mockHook("/nope/nada/iac", "/ nope / nada / iac", true, "status.json");

        Mockito.when(repository.findByCustomerNameAndProjectName("nope", "nada"))
                .thenReturn(Optional.of(MockUtils.mockMinimumEngagement("nope", "nada", null)));

        Mockito.when(gitApi.getStatus("nope", "nada")).thenReturn(MockUtils.mockStatus("green"));
        Mockito.when(gitApi.getCommits("nope", "nada"))
                .thenReturn(Lists.newArrayList(MockUtils.mockCommit("status.json", false)));

        Engagement updated = service.updateStatusAndCommits(hook);

        assertNotNull(updated);
        assertNotNull(updated.getStatus());
        assertNotNull(updated.getCommits());
        assertEquals(1, updated.getCommits().size());

        Mockito.verify(gitApi).getStatus("nope", "nada");
        Mockito.verify(repository).update(updated);

    }

    // getByCustomerAndProjectName

    @Test
    void testGetByCustomerNameAndProjectNameNotFound() {

        Mockito.when(
                repository.findByCustomerNameAndProjectName(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
                .thenReturn(Optional.empty());

        WebApplicationException wae = assertThrows(WebApplicationException.class,
                () -> service.getByCustomerAndProjectName("c1", "p1", Optional.empty()));
        assertEquals(404, wae.getResponse().getStatus());
        assertEquals("no engagement found with customer:project c1:p1", wae.getMessage());

    }

    @Test
    void testGetByCustomerNameAndProjectName() {

        Mockito.when(
                repository.findByCustomerNameAndProjectName(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
                .thenReturn(Optional.of(MockUtils.mockMinimumEngagement("c1", "p1", "1234")));

        Engagement e = service.getByCustomerAndProjectName("c1", "p1", Optional.empty());
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
                () -> service.getByUuid("1234", Optional.empty()));
        assertEquals(404, wae.getResponse().getStatus());
        assertEquals("no engagement found with id 1234", wae.getMessage());

    }

    @Test
    void testGetByUuid() {

        Mockito.when(repository.findByUuid("1234", Optional.empty()))
                .thenReturn(Optional.of(MockUtils.mockMinimumEngagement("c1", "p1", "1234")));

        Engagement e = service.getByUuid("1234", Optional.empty());
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

        service.getAll(categories, Optional.empty());

        Mockito.verify(repository).findAll(Optional.empty());
        Mockito.verify(repository, Mockito.times(0)).findByCategories(categories, Optional.empty());

    }

    @Test
    void testGetAllWithCategories() {

        service.getAll("hello", Optional.empty());

        Mockito.verify(repository, Mockito.times(0)).findAll(Optional.empty());
        Mockito.verify(repository).findByCategories("hello", Optional.empty());

    }

    // getSuggestions

    @Test
    void testGetSuggestionsNoneFound() {

        Mockito.when(repository.findCustomerSuggestions("c")).thenReturn(Lists.newArrayList());

        Collection<String> suggestions = service.getSuggestions("c");
        assertNotNull(suggestions);
        assertTrue(suggestions.isEmpty());

    }

    @Test
    void testGetSuggestions() {

        Engagement e1 = MockUtils.mockMinimumEngagement("customer1", "project1", "1234");
        Engagement e2 = MockUtils.mockMinimumEngagement("customer2", "project1", "5454");
        Mockito.when(repository.findCustomerSuggestions("c")).thenReturn(Lists.newArrayList(e1, e2));

        Collection<String> suggestions = service.getSuggestions("c");
        assertNotNull(suggestions);
        assertEquals(2, suggestions.size());
        assertTrue(suggestions.contains("customer1"));
        assertTrue(suggestions.contains("customer2"));

    }

    // deleteByCustomerAndProjectName

    @Test
    void testDeleteByCustomerAndProjectNameNotFound() {

        Mockito.when(repository.findByCustomerNameAndProjectName("c1", "p1", Optional.empty()))
                .thenReturn(Optional.empty());

        WebApplicationException wae = assertThrows(WebApplicationException.class,
                () -> service.deleteByCustomerAndProjectName("c1", "p1"));
        assertEquals(404, wae.getResponse().getStatus());
        assertEquals("no engagement found with customer:project c1:p1", wae.getMessage());

    }

    @Test
    void testDeleteByCustomerAndProjectName() {

        Engagement e = MockUtils.mockMinimumEngagement("c1", "p1", "1234");
        Mockito.when(repository.findByCustomerNameAndProjectName("c1", "p1", Optional.empty()))
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
        Mockito.when(repository.findByUuid("1234", Optional.empty())).thenReturn(Optional.of(e));

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
        Mockito.when(repository.findByUuid("1234", Optional.empty())).thenReturn(Optional.of(e));

        WebApplicationException wae = assertThrows(WebApplicationException.class,
                () -> service.deleteEngagement("1234"));
        assertEquals(400, wae.getResponse().getStatus());
        assertEquals("cannot delete engagement that has already been launched.", wae.getMessage());

    }

    @Test
    void testDeleteEngagement() {

        Engagement e = MockUtils.mockMinimumEngagement("c1", "p1", "1234");
        Mockito.when(repository.findByUuid("1234", Optional.empty())).thenReturn(Optional.of(e));

        service.deleteEngagement("1234");

        Mockito.verify(repository).delete(e);
        Mockito.verify(eventBus).sendAndForget(EventType.DELETE_ENGAGEMENT_EVENT_ADDRESS, e);

    }

    // getCategories

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " " })
    void testGetCategories(String match) {

        service.getCategories(match);

        Mockito.verify(repository).findAllCategoryWithCounts();

    }

    @Test
    void testGetCategories() {

        service.getCategories("match");

        Mockito.verify(repository).findCategorySuggestions("match");

    }

    // getArtifactTypes

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " " })
    void testGetArtifactTypes(String match) {

        service.getArtifactTypes(match);

        Mockito.verify(repository).findAllArtifactTypes();

    }

    @Test
    void testGetArtifactTypes() {

        service.getArtifactTypes("something");

        Mockito.verify(repository).findArtifactTypeSuggestions("something");

    }

    // setNullUuids

    @Test
    void testSetNullUuidsAlreadySet() {

        EngagementUser user1 = MockUtils.mockEngagementUser("b.s@example.com", "bill", "smith", "dev", "1234", false);
        EngagementUser user2 = MockUtils.mockEngagementUser("t.j@example.com", "tom", "jones", "dev", "4321", false);
        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "p2", null);
        e1.setEngagementUsers(Sets.newHashSet(user1, user2));

        EngagementUser user3 = MockUtils.mockEngagementUser("j.s@example.com", "jim", "smith", "dev", null, false);
        EngagementUser user4 = MockUtils.mockEngagementUser("t.j@example.com", "tom", "jones", "dev", "4321", false);
        Engagement e2 = MockUtils.mockMinimumEngagement("c1", "p2", "7890");
        e2.setEngagementUsers(Sets.newHashSet(user3, user4));

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

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "p1", "1234");
        Engagement e2 = MockUtils.mockMinimumEngagement("c2", "p2", "9876");
        Mockito.when(gitApi.getEngagments()).thenReturn(Lists.newArrayList(e1, e2));
        Mockito.when(repository.findByUuid("1234")).thenReturn(Optional.of(e1));

        service.syncGitToDatabase(true);

        Mockito.verify(repository).deleteAll();
        Mockito.verify(repository).persist(Mockito.anyIterable());

    }

    @Test
    void testSyncGitToDatabaseDoNotPurgeFirst() {

        Engagement e1 = MockUtils.mockMinimumEngagement("c1", "p1", "1234");
        Engagement e2 = MockUtils.mockMinimumEngagement("c2", "p2", "9876");
        Mockito.when(gitApi.getEngagments()).thenReturn(Lists.newArrayList(e1, e2));
        Mockito.when(repository.findByUuid("1234")).thenReturn(Optional.of(e1));

        service.syncGitToDatabase(false);

        Mockito.verify(repository, Mockito.times(0)).deleteAll();
        Mockito.verify(repository).persist(Mockito.anyIterable());

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

}