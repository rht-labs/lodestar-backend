package com.redhat.labs.omp.service;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.omp.model.FileAction;
import com.redhat.labs.omp.repository.ActiveSyncRepository;
import com.redhat.labs.omp.rest.client.OMPGitLabAPIService;

@ApplicationScoped
public class GitSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitSyncService.class);

    @Inject
    ActiveSyncRepository activeSyncRepository;

    @Inject
    EngagementService engagementService;

    @Inject
    @RestClient
    OMPGitLabAPIService gitApiClient;

    public void refreshBackedFromGit() {

        LOGGER.info("refreshing backend data from Git...");

        // get all engagements from git
        List<Engagement> engagementList = gitApiClient.getEngagments();

        // call sync
        engagementService.syncWithGitLab(engagementList);

        LOGGER.info("refresh of backend data complete.");

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

            try {

                // call git api
                Response response = gitApiClient.createOrUpdateEngagement(engagement, engagement.getLastUpdateByName(),
                        engagement.getLastUpdateByEmail());

                // update id for create actions
                if (FileAction.create == engagement.getAction()) {
                    updateIdFromResponse(engagement, response);
                }

                // reset modified
                engagement.setAction(null);

            } catch (WebApplicationException e) {
                // rest call returned and 400 or above http code
                LOGGER.error(
                        "failed to create or update engagement with message '{}', please check to see data needs to be refreshed from git. engagement: {}",
                        e.getMessage(), engagement);
                // go to next engagement to process
                continue;
            }

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
