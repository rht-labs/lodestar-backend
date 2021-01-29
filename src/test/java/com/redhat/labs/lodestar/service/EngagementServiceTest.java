package com.redhat.labs.lodestar.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.mockito.internal.util.collections.Sets;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.EngagementUser;
import com.redhat.labs.lodestar.model.GitlabProject;
import com.redhat.labs.lodestar.model.Hook;
import com.redhat.labs.lodestar.model.Launch;
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
        existing.setEngagementUsers(Sets.newSet(user1, user2));

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

        // requested update engagement
        EngagementUser user1 = mockEngagementUser("jj@example.com", "John", "Johnson", "admin", null);

        Engagement toUpdate = mockMinimumEngagement("c1", "p1", "00000");
        toUpdate.setEngagementUsers(Sets.newSet(user1));
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
        existing.setEngagementUsers(Sets.newSet(user1));

        // requested update engagement
        EngagementUser user2 = mockEngagementUser("js@example.com", "Jeff", "Smith", "observer", null);
        Engagement toUpdate = mockMinimumEngagement("c1", "p1", "00000");
        toUpdate.setEngagementUsers(Sets.newSet(user1, user2));
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
        e1.setEngagementUsers(Sets.newSet(user1));

        EngagementUser user2 = mockEngagementUser("js@example.com", "Jeff", "Smith", "observer", null);
        Engagement e2 = mockMinimumEngagement("c1", "p1", "5678");
        e2.setEngagementUsers(Sets.newSet(user2));

        Mockito.when(repository.streamAll()).thenReturn(Stream.of(e1, e2));

        assertEquals(1, engagementService.setNullUuids());

    }

    @Test
    void testSetNullUuidsOnEngagementAndEngagementUsersUuid() {

        EngagementUser user1 = mockEngagementUser("jj@example.com", "John", "Johnson", "admin", "1234");
        Engagement e1 = mockMinimumEngagement("c1", "p1", null);
        e1.setEngagementUsers(Sets.newSet(user1));

        EngagementUser user2 = mockEngagementUser("js@example.com", "Jeff", "Smith", "observer", null);
        Engagement e2 = mockMinimumEngagement("c1", "p1", "5678");
        e2.setEngagementUsers(Sets.newSet(user2));

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

    Engagement mockMinimumEngagement(String customerName, String projectName, String uuid) {
        return Engagement.builder().customerName(customerName).projectName(projectName).uuid(uuid).build();
    }

    EngagementUser mockEngagementUser(String email, String firstName, String lastName, String role, String uuid) {
        return EngagementUser.builder().email(email).firstName(firstName).lastName(lastName).role(role).uuid(uuid)
                .build();
    }

}
