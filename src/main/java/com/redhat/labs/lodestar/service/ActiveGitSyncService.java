package com.redhat.labs.lodestar.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.lodestar.model.ActiveSync;
import com.redhat.labs.lodestar.model.event.EventType;
import com.redhat.labs.lodestar.repository.ActiveSyncRepository;

import io.quarkus.panache.common.Sort;
import io.quarkus.scheduler.Scheduled;
import io.vertx.mutiny.core.eventbus.EventBus;
import lombok.Getter;

public class ActiveGitSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveGitSyncService.class);

    @Inject
    ActiveSyncRepository activeSyncRepository;

    @Inject
    EventBus eventBus;

    @Getter
    private final UUID uuid = UUID.randomUUID();

    private boolean active = false;
    private boolean performedUuidCheck = false;

    /**
     * Sets the active status to true if this application instance is performing the
     * sync processing. Will make itself active if the record goes stale in the db.
     * Otherwise, will set the active status to false which indicates it should not
     * be processing.
     * 
     * @return
     */
    @Scheduled(every = "10s")
    void checkIfActive() {

        // get record from db
        List<ActiveSync> syncRecordList = activeSyncRepository.listAll(Sort.ascending("lastUpdated"));

        if (syncRecordList.isEmpty()) {

            LOGGER.debug("no active found, inserting active record for instance {}", uuid);

            // try to insert the record to become the active instance
            activeSyncRepository.persist(new ActiveSync(uuid, LocalDateTime.now()));
            active = true;
            return;

        }

        // get control record
        ActiveSync sync = syncRecordList.remove(0);

        // if i created the record, update the time stamp
        if (uuid.equals(sync.getUuid())) {

            LOGGER.trace("i {} am active, updating check in timestamp.", uuid);
            // i am active, update time stamp
            sync.setLastUpdated(LocalDateTime.now());
            activeSyncRepository.update(sync);
            active = true;

            // shouldn't happen, but remove extra records if more than one exists
            if (!syncRecordList.isEmpty()) {

                LOGGER.debug("found multiple control records, performing cleanup");
                for (ActiveSync record : syncRecordList) {
                    activeSyncRepository.delete(record);
                }

            }

            return;

        }

        // i didn't create it, but check if last updated is stale
        if (sync.getLastUpdated().isBefore(LocalDateTime.now().minusSeconds(15))) {

            LOGGER.debug("timestamp exceeded, i {} am becoming active.", uuid);
            sync.setUuid(uuid);
            sync.setLastUpdated(LocalDateTime.now());
            activeSyncRepository.persistOrUpdate(sync);
            active = true;
            return;

        }

        LOGGER.debug("i {} am not active.", uuid);
        active = false;

    }

    @Scheduled(cron = "{auto.repopulate.cron.expr}")
    void repopulateDbIfEmpty() {

        // sync mongo with git if no engagements found in mongo
        if (active) {

            eventBus.sendAndForget(EventType.REFRESH_DATABASE_EVENT_ADDRESS, EventType.REFRESH_DATABASE_EVENT_ADDRESS);

        }

    }

    @Scheduled(every = "10s")
    void checkForNullUuids() {

        if (active && !performedUuidCheck) {
            // check for null uuids only once
            eventBus.sendAndForget(EventType.SET_UUID_EVENT_ADDRESS, EventType.SET_UUID_EVENT_ADDRESS);
            performedUuidCheck = true;
        }

    }

}
