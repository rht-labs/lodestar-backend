package com.redhat.labs.omp.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.omp.model.ActiveSync;
import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.omp.model.git.api.FileAction;
import com.redhat.labs.omp.model.git.api.GitApiEngagement;
import com.redhat.labs.omp.model.git.api.GitApiFile;
import com.redhat.labs.omp.repository.ActiveSyncRepository;
import com.redhat.labs.omp.rest.client.OMPGitLabAPIService;

import io.quarkus.panache.common.Sort;

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

    /**
     * Periodically processes modified {@link Engagement} in the data store by
     * calling the Git API to update Git.
     */
//    @Scheduled(every = "30s")
    void sendChangesToGitApi() {

        if (!pauseAutoSave.get() && active()) {
            processModifiedEngagements();
        }

    }

    /**
     * Periodically starts the process of pulling {@link Engagement} data from Git
     * and refreshing the data store.
     */
//    @Scheduled(every = "12h")
    void refreshDatabaseFromGit() {

        if (active()) {

            // set flag to stop other scheduler
            pauseAutoSave.set(true);

            // refresh data
            refreshBackedFromGit();

            // set flag to start other scheduler
            pauseAutoSave.set(false);

        }

    }

    /**
     * Processes modified {@link Engagement} in the data store by calling the Git
     * API to update Git.
     */
    public void processModifiedEngagements() {

        LOGGER.info("Sending changes from database to Git API...");

        // process creates first
        List<Engagement> createList = engagementService.getModifiedEngagementsByAction(FileAction.create);
        processModifiedEngagements(createList, FileAction.create);

        // process updated second
        List<Engagement> updatedList = engagementService.getModifiedEngagementsByAction(FileAction.update);
        processModifiedEngagements(updatedList, FileAction.update);

        // process deleted last
        List<Engagement> deletedList = engagementService.getModifiedEngagementsByAction(FileAction.delete);
        processModifiedEngagements(deletedList, FileAction.delete);

        LOGGER.info("All changes sent to Git API.");

    }

    public void refreshBackedFromGit() {

        LOGGER.info("refreshing backend data from Git...");

        // get all engagements from git
        List<GitApiEngagement> gitApiEngagementList = gitApiClient.getEngagments();

        // translate into engagments
        List<Engagement> engagementList = new ArrayList<>();
        gitApiEngagementList.stream().forEach((engagement) -> engagementList.add(Engagement.from(engagement)));

        // call sync
        engagementService.syncWithGitLab(engagementList);

        LOGGER.info("refresh of backend data complete.");

    }

    /**
     * Return true if this application instance is performing the sync processing.
     * Will make itself active if the record goes stale in the db. Otherwise, will
     * return false to indicate it should not be processing.
     * 
     * @return
     */
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

        // if i created the record, update the time stamp
        if (uuid.equals(sync.getUuid())) {

            // i am active, update time stamp
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

    /**
     * Processing the list of {@link Engagement} that have been modified. The Git
     * API will be called to process the engagement depending on the
     * {@link FileAction} specified. After the REST call completes, the
     * {@link Engagement} will be reset in the data store.
     * 
     * @param engagementList
     * @param action
     */
    private void processModifiedEngagements(List<Engagement> engagementList, FileAction action) {

        for (Engagement engagement : engagementList) {

            // reset modified
            engagement.setAction(null);

            // create file to update
            GitApiFile file = engagementService.createFileFromEngagement(engagement);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("...performing {} on file using Git API. {}", engagement.getAction().name(), file);
            }

            if (FileAction.create == action) {

                // call git api
                Response response = gitApiClient.createEngagement(GitApiEngagement.from(engagement),
                        engagement.getLastUpdateByName(), engagement.getLastUpdateByEmail());

                // update engagement id
                engagementService.updateEngagementId(engagement, response);

            } else if (FileAction.update == action) {

                // call git api
                gitApiClient.updateFile(engagement.getEngagementId(), file);

            } else if (FileAction.delete == action) {

                gitApiClient.deleteFile(engagement.getEngagementId(), file.getFilePath(),
                        engagement.getLastUpdateByName(), engagement.getLastUpdateByEmail());

            }

        }

        if (FileAction.create != action) {
            engagementService.updateEngagementListInRepository(engagementList);
        }

    }

}
