package com.redhat.labs.omp.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.omp.model.ActiveSync;
import com.redhat.labs.omp.repository.ActiveSyncRepository;

import io.quarkus.panache.common.Sort;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;

public class ActiveGitSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveGitSyncService.class);

    @Inject
    ActiveSyncRepository activeSyncRepository;

    @Inject
    GitSyncService gitSyncService;

    private final UUID uuid = UUID.randomUUID();

    private boolean active = false;

    /**
     * Check to see if application should take the active role for sync processes.
     * Then, requests that the database be refreshed if it does not contain data
     * already.
     * 
     * @param event
     */
    void onStart(@Observes StartupEvent event) {

        LOGGER.debug("starting instance {}", uuid);
        // try to set active flag
        checkIfActive();

    }

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

        if (syncRecordList.size() == 0) {

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

            LOGGER.debug("i {} am active, updating check in timestamp.", uuid);
            // i am active, update time stamp
            sync.setLastUpdated(LocalDateTime.now());
            activeSyncRepository.update(sync);
            active = true;

            // shouldn't happen, but remove extra records if more than one exists
            if (syncRecordList.size() > 0) {

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

    /**
     * Triggers push of modified engagements at scheduled timeframe to indicate all
     * modifications in the database should be pushed to Git.
     */
    @Scheduled(cron = "{auto.save.cron.expr}")
    void pushModififedEngagementsToGit() {

        if (active) {
            LOGGER.debug("scheduled job for send process time elapsed event triggered. {}", active);
            gitSyncService.processModifiedEngagements();
        }

    }

}
