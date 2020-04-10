package com.redhat.labs.omp.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.omp.model.ActiveSync;
import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.omp.model.git.api.FileAction;
import com.redhat.labs.omp.model.git.api.GitApiFile;
import com.redhat.labs.omp.repository.ActiveSyncRepository;
import com.redhat.labs.omp.rest.client.OMPGitLabAPIService;

import io.quarkus.panache.common.Sort;
import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class GitSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitSyncService.class);
    private final UUID uuid = UUID.randomUUID();

    AtomicBoolean pauseAutoSave = new AtomicBoolean(false);

    @Inject
    ActiveSyncRepository activeSyncRepository;

    @Inject
    EngagementService engagementService;

    @Inject
    @RestClient
    OMPGitLabAPIService gitApiClient;

    @Scheduled(every = "30s")
    void sendChangesToGitApi() {

        if (!pauseAutoSave.get() && active()) {

            LOGGER.info("Sending changes from database to Git API...");

            // get all documents with modified flag
            List<Engagement> modifiedList = engagementService.getModifiedEngagements();

            for (Engagement engagement : modifiedList) {

                // update flag to false
                engagement.setModified(false);

                // create file to update
                GitApiFile file = engagementService.createFileFromEngagement(engagement);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("...performing {} on file using Git API. {}", engagement.getAction().name(), file);
                }

                // send to git api
                if (FileAction.update == engagement.getAction()) {
                    gitApiClient.updateFile(engagement.getEngagementId(), file);
                } else if (FileAction.delete == engagement.getAction()) {
                    gitApiClient.deleteFile(engagement.getEngagementId(), file.getFilePath(),
                            engagement.getLastUpdateByName(), engagement.getLastUpdateByEmail());
                }

            }

            // update modified flag in db
            engagementService.updateEngagementListInRepository(modifiedList);

            LOGGER.info("All changes sent to Git API.");

        }

    }

    @Scheduled(every = "12h")
    void refreshDatabaseFromGit() {

        if (active()) {

            // set flag to stop other scheduler
            pauseAutoSave.set(true);

            // call sync
            engagementService.syncWithGitLab();

            // set flag to start other scheduler
            pauseAutoSave.set(false);

        }

    }

    boolean active() {

        // get record from db
        List<ActiveSync> syncRecordList = activeSyncRepository.listAll(Sort.ascending("lastUpdated"));

        if (syncRecordList.size() == 0) {

            // try to insert the record to become the active instance
            activeSyncRepository.persist(new ActiveSync(uuid, LocalDateTime.now()));
            return true;

        }

        // get control record
        ActiveSync sync = syncRecordList.remove(0);

        // shouldn't happen, but remove extra records if more than one exists
        if (syncRecordList.size() > 0) {

            for (ActiveSync record : syncRecordList) {
                activeSyncRepository.delete(record);
            }

        }

        // if i created the record, update the timestamp
        if (uuid.equals(sync.getUuid())) {

            // i am active, update timestamp
            sync.setLastUpdated(LocalDateTime.now());
            activeSyncRepository.update(sync);
            return true;

        }

        // i didn't create it, but check if last updated is stale
        if (sync.getLastUpdated().isBefore(LocalDateTime.now().minusMinutes(1))) {

            sync.setUuid(uuid);
            activeSyncRepository.persistOrUpdate(sync);
            return true;

        }

        return false;

    }

}
