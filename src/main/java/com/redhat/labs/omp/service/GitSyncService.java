package com.redhat.labs.omp.service;

import java.time.LocalDateTime;
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
import com.redhat.labs.omp.model.FileAction;
import com.redhat.labs.omp.repository.ActiveSyncRepository;
import com.redhat.labs.omp.rest.client.OMPGitLabAPIService;

import io.quarkus.panache.common.Sort;
import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class GitSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitSyncService.class);
    private final UUID uuid = UUID.randomUUID();

    AtomicBoolean autosave = new AtomicBoolean(true);

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
    @Scheduled(cron = "{auto.save.cron.expr}")
    void sendChangesToGitApi() {

        if (autosave.get() && active()) {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("backend instance {} is active and performing push to git.", uuid);
            }

            processModifiedEngagements();

        }

    }

    public boolean toggleAutoSave() {

        boolean temp;
        do {
            temp = autosave.get();
        } while(!autosave.compareAndSet(temp, !temp));

        return autosave.get();

    }

    public void refreshBackedFromGit() {

        LOGGER.info("refreshing backend data from Git...");

        // get all engagements from git
        List<Engagement> engagementList = gitApiClient.getEngagments();

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
     * Processes modified {@link Engagement} in the data store by calling the Git
     * API to update Git.
     */
    public void processModifiedEngagements() {
        processModifiedEngagements(engagementService.getModifiedEngagements());
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
    private void processModifiedEngagements(List<Engagement> engagementList) {

        for (Engagement engagement : engagementList) {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("...performing {} on engagement {} using Git API.", engagement.getAction().name(),
                        engagement);
            }

            // call git api
            Response response = gitApiClient.createOrUpdateEngagement(engagement, engagement.getLastUpdateByName(),
                    engagement.getLastUpdateByEmail());

            // update id for create actions
            if (FileAction.create == engagement.getAction()) {
                updateIdFromResponse(engagement, response);
            }

            // reset modified
            engagement.setAction(null);

        }

        // update engagements in db
        engagementService.updateEngagementListInRepository(engagementList);

    }

    /**
     * Sets the project ID from the Location header on the {@link Engagement}.
     * 
     * @param engagement
     * @param response
     */
    private void updateIdFromResponse(Engagement engagement, Response response) {

        // get location from header
        String location = response.getHeaderString("Location");
        // get id from location string
        String id = location.substring(location.lastIndexOf("/") + 1);

        // update engagement id
        engagement.setProjectId(Integer.valueOf(id));

    }

}
