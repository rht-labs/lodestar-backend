package com.redhat.labs.lodestar.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.redhat.labs.lodestar.model.Artifact;
import com.redhat.labs.lodestar.model.Category;
import com.redhat.labs.lodestar.model.Commit;
import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.EngagementUser;
import com.redhat.labs.lodestar.model.GitlabProject;
import com.redhat.labs.lodestar.model.Hook;
import com.redhat.labs.lodestar.model.HostingEnvironment;
import com.redhat.labs.lodestar.model.Launch;
import com.redhat.labs.lodestar.model.UseCase;
import com.redhat.labs.lodestar.repository.EngagementRepository;
import com.redhat.labs.lodestar.utils.EmbeddedMongoTest;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@EmbeddedMongoTest
@QuarkusTest
class EngagementServiceTest {

    @InjectMock
    EngagementRepository repository;

    @Inject
    EngagementService engagementService;

    @Test
    void testUpdateStatusInvalidProject() {
        Hook hook = Hook.builder().project(GitlabProject.builder().pathWithNamespace("/nope/nada/iac")
                .nameWithNamespace("/ nope / nada / iac").build()).build();

        Exception ex = assertThrows(WebApplicationException.class, () -> {
            engagementService.updateStatusAndCommits(hook);
        });

        assertEquals("no engagement found. unable to update from hook.", ex.getMessage());
    }

    @Test
    void testAlreadyLaunched() {
        Engagement engagement = Engagement.builder().launch(Launch.builder().build()).build();

        Exception ex = assertThrows(WebApplicationException.class, () -> {
            engagementService.launch(engagement);
        });

        assertEquals("engagement has already been launched.", ex.getMessage());
    }

    @Test
    void testUpdateRemoveAllUsers() {

        // persisted engagement
        EngagementUser user1 = mockEngagementUser("jj@example.com", "John", "Johnson", "admin", "1234");
        EngagementUser user2 = mockEngagementUser("js@example.com", "Jeff", "Smith", "observer", "6789");
        Engagement existing = mockMinimumEngagement("c1", "p1", "00000");
        existing.setEngagementUsers(Sets.newHashSet(user1, user2));
        existing.setProjectId(1234);

        // requested update engagement
        Engagement toUpdate = mockMinimumEngagement("c1", "p1", "00000");
        toUpdate.setLastUpdate(ZonedDateTime.now(ZoneId.of("Z")).toString());

        // repository mocks
        Mockito.when(repository.findByUuid("00000")).thenReturn(Optional.of(existing));
        Mockito.when(repository.updateEngagementIfLastUpdateMatched(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(toUpdate));

        Engagement response = engagementService.update(toUpdate);

        assertNotNull(response);
        assertEquals("c1", response.getCustomerName());
        assertEquals("p1", response.getProjectName());
        assertNull(response.getEngagementUsers());

    }

    @Test
    void testUpdateAllNewUsers() {

        // persisted engagement
        Engagement existing = mockMinimumEngagement("c1", "p1", "00000");
        existing.setProjectId(1234);

        // requested update engagement
        EngagementUser user1 = mockEngagementUser("jj@example.com", "John", "Johnson", "admin", null);

        Engagement toUpdate = mockMinimumEngagement("c1", "p1", "00000");
        toUpdate.setEngagementUsers(Sets.newHashSet(user1));
        toUpdate.setLastUpdate(ZonedDateTime.now(ZoneId.of("Z")).toString());

        // repository mocks
        Mockito.when(repository.findByUuid("00000")).thenReturn(Optional.of(existing));
        Mockito.when(repository.updateEngagementIfLastUpdateMatched(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(toUpdate));

        Engagement response = engagementService.update(toUpdate);

        assertNotNull(response);
        assertEquals("c1", response.getCustomerName());
        assertEquals("p1", response.getProjectName());
        assertNotNull(response.getEngagementUsers());
        assertEquals(1, response.getEngagementUsers().size());

        EngagementUser actualUser = response.getEngagementUsers().iterator().next();
        assertNotNull(actualUser);
        assertEquals("jj@example.com", actualUser.getEmail());
        assertEquals("John", actualUser.getFirstName());
        assertEquals("Johnson", actualUser.getLastName());
        assertEquals("admin", actualUser.getRole());
        assertNotNull(actualUser.getUuid());

    }

    @Test
    void testUpdateNewAndExistingUsers() {

        // persisted engagement
        EngagementUser user1 = mockEngagementUser("jj@example.com", "John", "Johnson", "admin", "1234");
        Engagement existing = mockMinimumEngagement("c1", "p1", "00000");
        existing.setEngagementUsers(Sets.newHashSet(user1));
        existing.setProjectId(1234);

        // requested update engagement
        EngagementUser user2 = mockEngagementUser("js@example.com", "Jeff", "Smith", "observer", null);
        Engagement toUpdate = mockMinimumEngagement("c1", "p1", "00000");
        toUpdate.setEngagementUsers(Sets.newHashSet(user1, user2));
        toUpdate.setLastUpdate(ZonedDateTime.now(ZoneId.of("Z")).toString());

        // repository mocks
        Mockito.when(repository.findByUuid("00000")).thenReturn(Optional.of(existing));
        Mockito.when(repository.updateEngagementIfLastUpdateMatched(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(toUpdate));

        Engagement response = engagementService.update(toUpdate);

        assertNotNull(response);
        assertEquals("c1", response.getCustomerName());
        assertEquals("p1", response.getProjectName());
        assertNotNull(response.getEngagementUsers());
        assertEquals(2, response.getEngagementUsers().size());

        for (EngagementUser user : response.getEngagementUsers()) {

            if (user.getEmail().equals("jj@example.com")) {
                assertEquals("jj@example.com", user.getEmail());
                assertEquals("John", user.getFirstName());
                assertEquals("Johnson", user.getLastName());
                assertEquals("admin", user.getRole());
                assertEquals("1234", user.getUuid());
            } else {
                assertEquals("js@example.com", user.getEmail());
                assertEquals("Jeff", user.getFirstName());
                assertEquals("Smith", user.getLastName());
                assertEquals("observer", user.getRole());
                assertNotNull(user.getUuid());
            }

        }

    }

    @Test
    void testSetNullUuidsEngagementUuid() {

        Engagement e1 = mockMinimumEngagement("c1", "p1", null);
        Engagement e2 = mockMinimumEngagement("c1", "p1", "5678");

        Mockito.when(repository.streamAll()).thenReturn(Stream.of(e1, e2));

        assertEquals(1, engagementService.setNullUuids());

    }

    @Test
    void testSetNullUuidsEngagementUsersUuid() {

        EngagementUser user1 = mockEngagementUser("jj@example.com", "John", "Johnson", "admin", "1234");
        Engagement e1 = mockMinimumEngagement("c1", "p1", "1234");
        e1.setEngagementUsers(Sets.newHashSet(user1));

        EngagementUser user2 = mockEngagementUser("js@example.com", "Jeff", "Smith", "observer", null);
        Engagement e2 = mockMinimumEngagement("c1", "p1", "5678");
        e2.setEngagementUsers(Sets.newHashSet(user2));

        Mockito.when(repository.streamAll()).thenReturn(Stream.of(e1, e2));

        assertEquals(1, engagementService.setNullUuids());

    }

    @Test
    void testSetNullUuidsOnEngagementAndEngagementUsersUuid() {

        EngagementUser user1 = mockEngagementUser("jj@example.com", "John", "Johnson", "admin", "1234");
        Engagement e1 = mockMinimumEngagement("c1", "p1", null);
        e1.setEngagementUsers(Sets.newHashSet(user1));

        EngagementUser user2 = mockEngagementUser("js@example.com", "Jeff", "Smith", "observer", null);
        Engagement e2 = mockMinimumEngagement("c1", "p1", "5678");
        e2.setEngagementUsers(Sets.newHashSet(user2));

        Mockito.when(repository.streamAll()).thenReturn(Stream.of(e1, e2));

        assertEquals(2, engagementService.setNullUuids());

    }

    @Test
    void testDeleteEngagementDoesNotExist() {

        Mockito.when(repository.findByUuid("123", Optional.empty())).thenReturn(Optional.empty());
        
        Exception ex = assertThrows(WebApplicationException.class, () -> {
            engagementService.deleteEngagement("123");
        });

        assertEquals("no engagement found with id 123", ex.getMessage());

    }

    @Test
    void testDeleteEngagementAlreadyLaunched() {

        Engagement e = mockMinimumEngagement("c1", "p1", "123");
        e.setLaunch(Launch.builder().build());
        Mockito.when(repository.findByUuid("123", Optional.empty())).thenReturn(Optional.of(e));
        
        Exception ex = assertThrows(WebApplicationException.class, () -> {
            engagementService.deleteEngagement("123");
        });

        assertEquals("cannot delete engagement that has already been launched.", ex.getMessage());

    }

    @Test
    void testDeleteEngagementSuccess() {

        Engagement e = mockMinimumEngagement("c1", "p1", "123");
        Mockito.when(repository.findByUuid("123", Optional.empty())).thenReturn(Optional.of(e));

        engagementService.deleteEngagement("123");

        Mockito.verify(repository).delete(e);

    }

    @Test
    void testClone() {
        
        Engagement e = Engagement.builder().build();
        Engagement clone = engagementService.clone(e);

        assertNotSame(e, clone);
        
    }

    @Test
    void testDeepCopyWithSubCollections() {

        Engagement e = Engagement.builder().hostingEnvironments(Lists.newArrayList(createHostingEnvironment()))
                .engagementUsers(Sets.newHashSet(createEngagementUser())).commits(Lists.newArrayList(createCommit()))
                .categories(Lists.newArrayList(createCategory())).useCases(Lists.newArrayList(createUseCase()))
                .artifacts(Lists.newArrayList(createArtifact())).build();

        Engagement copy = engagementService.clone(e);

        assertNotSame(e, copy);

        // hosting environments
        assertNotSame(e.getHostingEnvironments(), copy.getHostingEnvironments());
        assertEquals(1, e.getHostingEnvironments().size());
        assertEquals(1, copy.getHostingEnvironments().size());
        assertNotSame(e.getHostingEnvironments().get(0), copy.getHostingEnvironments().get(0));

        // engagement users
        assertNotSame(e.getEngagementUsers(), copy.getEngagementUsers());
        assertEquals(1, e.getEngagementUsers().size());
        assertEquals(1, copy.getEngagementUsers().size());
        assertNotSame(e.getEngagementUsers().iterator().next(), copy.getEngagementUsers().iterator().next());

        // commits
        assertNotSame(e.getCommits(), copy.getCommits());
        assertEquals(1, e.getCommits().size());
        assertEquals(1, copy.getCommits().size());
        assertNotSame(e.getCommits().get(0), copy.getCommits().get(0));

        // categories
        assertNotSame(e.getCategories(), copy.getCategories());
        assertEquals(1, e.getCategories().size());
        assertEquals(1, copy.getCategories().size());
        assertNotSame(e.getCategories().get(0), copy.getCategories().get(0));

        // use case
        assertNotSame(e.getUseCases(), copy.getUseCases());
        assertEquals(1, e.getUseCases().size());
        assertEquals(1, copy.getUseCases().size());
        assertNotSame(e.getUseCases().get(0), copy.getUseCases().get(0));

        // artifacts
        assertNotSame(e.getArtifacts(), copy.getArtifacts());
        assertEquals(1, e.getArtifacts().size());
        assertEquals(1, copy.getArtifacts().size());
        assertNotSame(e.getArtifacts().get(0), copy.getArtifacts().get(0));

    }

    HostingEnvironment createHostingEnvironment() {
        return HostingEnvironment.builder().environmentName("some-environment").build();
    }

    EngagementUser createEngagementUser() {
        return EngagementUser.builder().firstName("bob").lastName("smith").build();
    }

    Commit createCommit() {
        return Commit.builder().id("1234").build();
    }

    Category createCategory() {
        return Category.builder().name("cat1").build();
    }

    UseCase createUseCase() {
        return UseCase.builder().id("4321").build();
    }

    Artifact createArtifact() {
        return Artifact.builder().title("art1").build();
    }

    Engagement mockMinimumEngagement(String customerName, String projectName, String uuid) {
        return Engagement.builder().customerName(customerName).projectName(projectName).uuid(uuid).build();
    }

    EngagementUser mockEngagementUser(String email, String firstName, String lastName, String role, String uuid) {
        return EngagementUser.builder().email(email).firstName(firstName).lastName(lastName).role(role).uuid(uuid)
                .build();
    }

}
