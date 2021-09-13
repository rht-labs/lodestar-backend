package com.redhat.labs.lodestar.service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import com.redhat.labs.lodestar.rest.client.EngagementStatusApiClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.event.EventType;
import com.redhat.labs.lodestar.model.event.RetriableEvent;
import com.redhat.labs.lodestar.rest.client.ActivityApiClient;

import io.quarkus.vertx.ConsumeEvent;
import io.vertx.mutiny.core.eventbus.EventBus;

public class EventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventService.class);

    private static final String LAST_PAGE_HEADER = "x-last-page";

    @ConfigProperty(name = "event.retry.delay.factor", defaultValue = "2")
    Integer eventRetryDelayFactor;

    @ConfigProperty(name = "event.retry.max.delay", defaultValue = "60")
    Integer eventRetryMaxDelay;

    @Inject
    @RestClient
    ActivityApiClient activityApiClient;

    @Inject
    @RestClient
    EngagementStatusApiClient engagementStatusApiClient;

    @Inject
    EngagementService engagementService;

    @Inject
    EventBus eventBus;

    @Inject
    Jsonb jsonb;


    /**
     * Sends the given {@link Engagement} to be processed if the max retry limit has
     * not been exceeded and the {@link Engagement} has not been updated since the
     * original update request.
     * 
     * @param event
     */
    @ConsumeEvent(value = EventType.RETRY_UPDATE_EVENT_ADDRESS, blocking = true)
    void consumeRetryUpdateEvent(RetriableEvent event) {
        retryEvent(event, () -> {

            Engagement engagement = event.getEngagement();

            try {

                // get current engagement from db
                Engagement persisted = engagementService.getByUuid(engagement.getUuid());

                // only resend if exists and not updated
                if (null != persisted && persisted.getLastUpdate().equals(engagement.getLastUpdate())) {
                    createOrUpdateEngagement(event, false);
                }

            } catch (WebApplicationException wae) {
                // exit if engagement deleted after event sent
                if (wae.getResponse().getStatus() == 404) {
                    LOGGER.info("cancelling retry event because engagement with id {} not found in db.",
                            engagement.getUuid());
                }
            }

        });

    }

    /**
     * Sends the given {@link Engagement} to be processed if the max retry limit has
     * not been exceeded.
     * 
     * @param event
     */
    @ConsumeEvent(value = EventType.RETRY_DELETE_EVENT_ADDRESS, blocking = true)
    void consumeRetryDeleteEvent(RetriableEvent event) {
        LOGGER.info("Event {} disabled", EventType.RETRY_DELETE_EVENT_ADDRESS);
    }

    /**
     * Runs the given function if the event max retry has not been exceeded.
     * 
     * @param event
     * @param runnable
     */
    void retryEvent(RetriableEvent event, Runnable runnable) {

        if (event.shouldRetry()) {

            // perform sleep to delay retry
            int seconds = event.getCurrentRetryCount() * eventRetryDelayFactor;
            if (seconds > eventRetryMaxDelay) {
                seconds = eventRetryMaxDelay;
            }

            try {
                TimeUnit.SECONDS.sleep(seconds);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // run configured method
            runnable.run();

        }

    }

    /**
     * Calls the create or update API with the given {@link Engagement}. If the
     * isCreate flag is true, it will update the project ID in the database based on
     * the API response.
     * 
     * If the API call produces an error code, the {@link RetriableEvent} will be
     * sent to the {@link EventBus} for reprocessing.
     * 
     * @param event
     * @param isCreate
     */
    private void createOrUpdateEngagement(RetriableEvent event, boolean isCreate) {
        LOGGER.debug("gitlab create disabled");
    }

    /**
     * Sets the project ID from the Location header on the {@link Engagement}.
     *
     * @param response
     */
    private Integer getProjectIdFromResponse(Response response) {

        // get location from header
        String location = response.getHeaderString("Location");
        // get id from location string
        String id = location.substring(location.lastIndexOf("/") + 1);

        return Integer.valueOf(id);

    }

    /**
     * Retrieves the last page number from header of the {@link Response}.
     * 
     * @param response
     * @return
     */
    private Integer getNumberOfPages(Response response) {

        String lastPage = response.getHeaderString(LAST_PAGE_HEADER);
        return lastPage == null ? 1 : Integer.valueOf(lastPage);

    }

    /**
     * Reads the {@link List} of {@link Engagement}s from the {@link Response}
     * entity.
     * 
     * @param response
     * @return
     */
    private List<Engagement> getEngagementsFromResponse(Response response) {

        return response.readEntity(new GenericType<List<Engagement>>() {
        });

    }

    /**
     * Returns the Response from the Git API for the provided page number.
     * 
     * @param page
     * @return
     */
    private Response getPageOfEngagements(Integer page) {

        LOGGER.info("getting page {} of engagements from git api. DISABLED", page);
        return Response.ok("[]").build();
    }


    /**
     * Gets the engagement from the Git API and sends event to delete and reinsert.
     * 
     * @param projectId id of the project
     */
    @ConsumeEvent(value = EventType.DELETE_AND_RELOAD_ENGAGEMENT_EVENT_ADDRESS)
    void consumeDeleteAndReLoadEngagementEvent(String projectId) {
        LOGGER.info("Event {} disabled", EventType.DELETE_AND_RELOAD_ENGAGEMENT_EVENT_ADDRESS);

    }

    /**
     * Removes the {@link Engagement} based on UUID and sends event to reinsert.
     * 
     * @param engagement
     */
    @ConsumeEvent(value = EventType.DELETE_ENGAGEMENT_FROM_DATABASE_EVENT_ADDRESS)
    void consumeDeleteEngagementFromDatabaseEvent(Engagement engagement) {
        LOGGER.warn("Removed this event. Check for comparable in engagement service");
    }

    /**
     * Starts the process of inserting any {@link Engagement}s in Git that are not
     * in the database. Existing {@link Engagement}s will not be updated.
     * 
     * @param event
     */
    @ConsumeEvent(value = EventType.LOAD_DATABASE_EVENT_ADDRESS, blocking = true)
    void consumeLoadDatabaseEvent(String event) {
        LOGGER.warn("Disabled consumeLoadDatabaseEvent");
    }

    /**
     * Get all {@link Engagement}s for the specified page and send event to process
     * each one.
     * 
     * @param pageNumber
     */
    @ConsumeEvent(value = EventType.GET_PAGE_OF_ENGAGEMENTS_EVENT_ADDRESS, blocking = true)
    void consumeGetPageEvent(Integer pageNumber) {
        LOGGER.info("Event {} disabled", EventType.GET_PAGE_OF_ENGAGEMENTS_EVENT_ADDRESS);

    }

    /**
     * Creates a PERSIST_ENGAGEMENT_EVENT for each {@link Engagement} in the
     * provided {@link List}.
     * 
     * @param engagementsJson
     */
    @ConsumeEvent(value = EventType.PERSIST_ENGAGEMENT_LIST_EVENT_ADDRESS)
    void consumeProcessEngagementList(String engagementsJson) {
        LOGGER.info("Event {} disabled", EventType.PERSIST_ENGAGEMENT_LIST_EVENT_ADDRESS);

    }

    /**
     * Persists the {@link Engagement} in the database if it does not already exist.
     * 
     * @param engagement
     */
    @ConsumeEvent(value = EventType.PERSIST_ENGAGEMENT_EVENT_ADDRESS)
    void consumeInsertIfMissingEvent(Engagement engagement) {

        LOGGER.debug("Removed. Check engagement service");
    }


    //TODO these should be moved to services that control engagement status and activity
    @ConsumeEvent(value = EventType.RELOAD_ENGAGEMENT_STATUS_EVENT_ADDRESS, blocking = true)
    void consumeEngagementStatusReloadEvent(String name) {
        LOGGER.debug("refresh {}", name);
        engagementStatusApiClient.refresh();
        LOGGER.debug("refresh {} completed", name);
    }
    
    @ConsumeEvent(value = EventType.RELOAD_ACTIVITY_EVENT_ADDRESS, blocking = true)
    void consumeActivityReloadEvent(String name) {
        LOGGER.debug("refresh {}", name);
        activityApiClient.refresh();
        LOGGER.debug("refresh {} completed", name);
    }

}