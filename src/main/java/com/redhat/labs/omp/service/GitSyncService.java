package com.redhat.labs.omp.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.omp.model.CreationDetails;
import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.omp.model.FileAction;
import com.redhat.labs.omp.model.event.BackendEvent;
import com.redhat.labs.omp.model.event.EventType;
import com.redhat.labs.omp.rest.client.OMPGitLabAPIService;

import io.quarkus.vertx.ConsumeEvent;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.eventbus.EventBus;

@ApplicationScoped
public class GitSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitSyncService.class);

    @Inject
    @RestClient
    OMPGitLabAPIService gitApiClient;

    @Inject
    Vertx vertx;

    @Inject
    EventBus eventBus;

    /**
     * This method consumes a {@link BackendEvent}, enriches the event with
     * {@link Engagement}s from Git and sends the event to the {@link EventBus} for
     * processing.
     * 
     * @param event
     */
    @ConsumeEvent(EventType.Constants.DB_REFRESH_REQUESTED_ADDRESS)
    void consumeDbRefreshRequestedEvent(BackendEvent event) {

        LOGGER.debug("processing db refresh request event");

        vertx.<List<Engagement>>executeBlocking(promise -> {

            try {
                List<Engagement> engagementList = gitApiClient.getEngagments();
                promise.complete(engagementList);
            } catch (WebApplicationException wae) {
                promise.fail(wae);
            }

        }).subscribe().with(item -> {
            // create refresh event
            BackendEvent refreshDbEvent = BackendEvent.createDatabaseRefreshEvent(item, event.isForceUpdate());
            // send event to bus for processing
            eventBus.sendAndForget(refreshDbEvent.getEventType().getEventBusAddress(), refreshDbEvent);
        }, failure -> {
            LOGGER.warn("failed to retrieve engagements from git. {}", failure.getMessage(), failure);
        });

    }

    /**
     * Consumes {@link BackendEvent} to trigger the processing of modified
     * {@link Engagement}s. Starts the processing in a separate {@link Thread} using
     * executeBlocking to prevent blocking the event loop.
     * 
     * @param event
     */
    @ConsumeEvent(EventType.Constants.UPDATE_ENGAGEMENTS_IN_GIT_REQUESTED_ADDRESS)
    void consumeUpdateEngagementsInGitRequestedEvent(BackendEvent event) {

        List<Engagement> engagementList = event.getEngagementList();

        vertx.executeBlocking(promise -> {
            try {
                processModifiedEvents(engagementList);
                promise.complete();
            } catch (Exception e) {
                promise.fail(e);
            }
        }).subscribe().with(item -> {
            LOGGER.debug("processing modified engagements succeeded.");
        }, failure -> {
            LOGGER.warn("processing modified engagements failed with message {}", failure.getMessage(), failure);
        });

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

                // reset modified
                engagement.setAction(null);
                // make sure the connection is closed
                response.close();

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
        BackendEvent updateInDbEvent = BackendEvent.createUpdateEngagementsInDbRequestedEvent(engagementList);
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