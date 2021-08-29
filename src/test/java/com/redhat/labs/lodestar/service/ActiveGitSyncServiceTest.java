package com.redhat.labs.lodestar.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.common.collect.Lists;
import com.redhat.labs.lodestar.model.ActiveSync;
import com.redhat.labs.lodestar.model.event.EventType;
import com.redhat.labs.lodestar.repository.ActiveSyncRepository;

import io.vertx.mutiny.core.eventbus.EventBus;

class ActiveGitSyncServiceTest {

    ActiveSyncRepository repository;
    EventBus eventBus;
    ActiveGitSyncService service;

    @BeforeEach
    void setup() {

        repository = Mockito.mock(ActiveSyncRepository.class);
        eventBus = Mockito.mock(EventBus.class);

        service = new ActiveGitSyncService();
        service.activeSyncRepository = repository;
        service.eventBus = eventBus;

    }

    @AfterEach
    void tearDown() {
        Mockito.reset(repository, eventBus);
    }

    @Test
    void testCheckIfActiveNoExistingRecord() {

        service.checkIfActive();

        Mockito.verify(repository, Mockito.times(1)).persist(Mockito.any(ActiveSync.class));

    }

    @Test
    void testCheckIfActiveExistingRecordAlreayActive() {

        ActiveSync record = new ActiveSync(service.getUuid(), LocalDateTime.now());
        Mockito.when(repository.listAll(Mockito.any())).thenReturn(Lists.newArrayList(record));

        service.checkIfActive();

        Mockito.verify(repository, Mockito.times(1)).update(Mockito.any(ActiveSync.class));
        Mockito.verify(repository, Mockito.times(0)).delete(Mockito.any(ActiveSync.class));

    }

    @Test
    void testCheckIfActiveExistingRecordAlreayActiveMultipleRecords() {

        ActiveSync record1 = new ActiveSync(service.getUuid(), LocalDateTime.now());
        ActiveSync record2 = new ActiveSync(UUID.randomUUID(), LocalDateTime.now());
        Mockito.when(repository.listAll(Mockito.any())).thenReturn(Lists.newArrayList(record1, record2));

        service.checkIfActive();

        Mockito.verify(repository, Mockito.times(1)).update(Mockito.any(ActiveSync.class));
        Mockito.verify(repository, Mockito.times(1)).delete(Mockito.any(ActiveSync.class));

    }

    @Test
    void testCheckIfActiveExistingExpiredRecord() {

        ActiveSync record1 = new ActiveSync(UUID.randomUUID(), LocalDateTime.now().minusSeconds(20));
        Mockito.when(repository.listAll(Mockito.any())).thenReturn(Lists.newArrayList(record1));

        service.checkIfActive();

        Mockito.verify(repository, Mockito.times(0)).update(Mockito.any(ActiveSync.class));
        Mockito.verify(repository, Mockito.times(0)).delete(Mockito.any(ActiveSync.class));
        Mockito.verify(repository, Mockito.times(1)).persistOrUpdate(Mockito.any(ActiveSync.class));

    }

    @Test
    void testCheckIfActiveNotActive() {

        ActiveSync record1 = new ActiveSync(UUID.randomUUID(), LocalDateTime.now());
        Mockito.when(repository.listAll(Mockito.any())).thenReturn(Lists.newArrayList(record1));

        service.checkIfActive();

        Mockito.verify(repository, Mockito.times(0)).update(Mockito.any(ActiveSync.class));
        Mockito.verify(repository, Mockito.times(0)).delete(Mockito.any(ActiveSync.class));
        Mockito.verify(repository, Mockito.times(0)).persistOrUpdate(Mockito.any(ActiveSync.class));

    }

    @Test
    void testRepopulateDbIfEmptyNotActive() {

        service.repopulateDbIfEmpty();

        Mockito.verify(eventBus,
                Mockito.times(0)).publish(Mockito.eq(EventType.LOAD_DATABASE_EVENT_ADDRESS), Mockito.any());

    }

    @Test
    void testRepopulateDbIfEmptyActive() {

        ActiveSync record = new ActiveSync(service.getUuid(), LocalDateTime.now());
        Mockito.when(repository.listAll(Mockito.any())).thenReturn(Lists.newArrayList(record));

        service.checkIfActive();

        service.repopulateDbIfEmpty();

        Mockito.verify(eventBus).publish(Mockito.eq(EventType.LOAD_DATABASE_EVENT_ADDRESS), Mockito.any());

    }

    @Test
    void testCheckForNullUuidsNotActive() {

        service.checkForNullUuids();

        Mockito.verify(eventBus,
                Mockito.times(0)).publish(Mockito.eq(EventType.SET_UUID_EVENT_ADDRESS), Mockito.any());

    }

    @Test
    void testCheckForNullUuidsActive() {

        ActiveSync record = new ActiveSync(service.getUuid(), LocalDateTime.now());
        Mockito.when(repository.listAll(Mockito.any())).thenReturn(Lists.newArrayList(record));

        service.checkIfActive();

        // call to send event first time
        service.checkForNullUuids();

        // call again to verify only one event sent

        Mockito.verify(eventBus,
                Mockito.times(1)).publish(Mockito.eq(EventType.SET_UUID_EVENT_ADDRESS), Mockito.any());

    }

}