package com.redhat.labs.lodestar.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.lodestar.model.CreationDetails;
import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.FileAction;
import com.redhat.labs.lodestar.model.event.BackendEvent;
import com.redhat.labs.lodestar.model.event.EventType;
import com.redhat.labs.lodestar.rest.client.LodeStarGitLabAPIService;

import io.quarkus.vertx.ConsumeEvent;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.eventbus.EventBus;

@ApplicationScoped
public class GitSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitSyncService.class);

    @Inject
    @RestClient
    LodeStarGitLabAPIService gitApiClient;

    @Inject
    Vertx vertx;

    @Inject
    EventBus eventBus;

    /**
     * Consumes {@link BackendEvent} to trigger the processing of modified
     * {@link Engagement}s.
     * 
     * @param event
     */
    @ConsumeEvent(value = EventType.Constants.UPDATE_ENGAGEMENTS_IN_GIT_REQUESTED_ADDRESS, blocking = true)
    void consumeUpdateEngagementsInGitRequestedEvent(BackendEvent event) {
        List<Engagement> engagementList = event.getEngagementList();
        processModifiedEvents(engagementList);
    }

    /**
     * Consumes a delete engagement event.
     * 
     * @param engagement
     */
    @ConsumeEvent(EventType.Constants.DELETE_ENGAGEMENT_IN_GIT_REQUESTED_ADDRESS)
    void consumeDeleteEngagementInGitRequestedEvent(Engagement engagement) {
        gitApiClient.deleteEngagement(engagement.getCustomerName(), engagement.getProjectName());
    }

    /**
     * Processes given the list of {@link Engagement} that have been modified. The
     * Git API will be called to process the engagement depending on the
     * {@link FileAction} specified. After the REST call completes, the
     * {@link Engagement} updated list will be added to a {@link BackendEvent} and
     * added to the {@link EventBus} to trigger an updated in the database.
     * 
     * @param engagementList
     */
    private void processModifiedEvents(List<Engagement> engagementList) {

        List<Engagement> processed = new ArrayList<>();

        for (Engagement engagement : engagementList) {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("...performing {} on engagement {} using Git API.", engagement.getAction().name(),
                        engagement);
            }

            // set creation details for create actions
            if (FileAction.create == engagement.getAction()) {
                setCreationDetails(engagement);
            }

            try (Response response = gitApiClient.createOrUpdateEngagement(engagement, engagement.getLastUpdateByName(),
                    engagement.getLastUpdateByEmail())) {

                // update id for create actions
                if (FileAction.create == engagement.getAction()) {
                    updateIdFromResponse(engagement, response);
                }

                // add to processed list if successful
                processed.add(engagement);

            } catch (WebApplicationException e) {
                // rest call returned and 400 or above http code
                LOGGER.error(
                        "failed to create or update engagement with message '{}', please check to see data needs to be refreshed from git. engagement: {}",
                        e.getMessage(), engagement);
                // go to next engagement to process
                continue;
            }

        }

        // send event to update in db
        BackendEvent updateInDbEvent = BackendEvent.createUpdateEngagementsInDbRequestedEvent(processed);
        eventBus.sendAndForget(updateInDbEvent.getEventType().getEventBusAddress(), updateInDbEvent);

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

    private void setCreationDetails(Engagement engagement) {

        // set creation details
        CreationDetails creationDetails = CreationDetails.builder().createdByUser(engagement.getLastUpdateByName())
                .createdByEmail(engagement.getLastUpdateByEmail())
                .createdOn(ZonedDateTime.now(ZoneId.of("Z")).toString()).build();
        engagement.setCreationDetails(creationDetails);

    }

}