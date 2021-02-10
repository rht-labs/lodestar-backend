package com.redhat.labs.lodestar.service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.EngagementUser;
import com.redhat.labs.lodestar.model.event.EventType;
import com.redhat.labs.lodestar.model.event.RetriableEvent;
import com.redhat.labs.lodestar.model.event.RetriableEvent.RetriableEventBuilder;
import com.redhat.labs.lodestar.rest.client.LodeStarGitLabAPIService;

import io.quarkus.vertx.ConsumeEvent;
import io.vertx.mutiny.core.eventbus.EventBus;

public class EventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventService.class);

    @ConfigProperty(name = "event.max.retries", defaultValue = "-1")
    Integer eventMaxRetries;

    @ConfigProperty(name = "event.retry.delay.factor", defaultValue = "2")
    Integer eventRetryDelayFactor;

    @Inject
    @RestClient
    LodeStarGitLabAPIService gitApiClient;

    @Inject
    EngagementService engagementService;

    @Inject
    EventBus eventBus;

    /**
     * Wraps the {@link Engagement} in a {@link RetriableEvent} and starts
     * processing create API call.
     * 
     * @param engagement
     */
    @ConsumeEvent(value = EventType.CREATE_ENGAGEMENT_EVENT_ADDRESS, blocking = true)
    void consumeCreateEngagementEvent(Engagement engagement) {
        RetriableEvent event = buildRetriableEvent(engagement);
        createOrUpdateEngagement(event, true);
    }

    /**
     * Wraps the {@link Engagement} in a {@link RetriableEvent} and starts
     * processing update API call.
     * 
     * @param engagement
     */
    @ConsumeEvent(value = EventType.UPDATE_ENGAGEMENT_EVENT_ADDRESS, blocking = true)
    void consumeUpdateEngagementEvent(Engagement engagement) {
        RetriableEvent event = buildRetriableEvent(engagement);
        createOrUpdateEngagement(event, false);
    }

    /**
     * Wraps the {@link Engagement} in a {@link RetriableEvent} and starts
     * processing the delete API call.
     * 
     * @param engagement
     */
    @ConsumeEvent(EventType.DELETE_ENGAGEMENT_EVENT_ADDRESS)
    void consumeDeleteEngagementEvent(Engagement engagement) {
        RetriableEvent event = buildRetriableEvent(engagement);
        deleteEngagement(event);
    }

    /**
     * Starts the process of inserting any {@link Engagement}s in Git that are not
     * in the database. Existing {@link Engagement}s will not be udpated.
     * 
     * @param event
     */
    @ConsumeEvent(value = EventType.REFRESH_DATABASE_EVENT_ADDRESS, blocking = true)
    void consumeRefreshDatabaseEvent(String event) {
        engagementService.syncGitToDatabase(false);
    }

    /**
     * Starts the process of setting UUIDs for any {@link Engagement} or
     * {@link EngagementUser} that has a null value for UUID.
     * 
     * @param event
     */
    @ConsumeEvent(value = EventType.SET_UUID_EVENT_ADDRESS, blocking = true)
    void setUuidEvent(String event) {
        engagementService.setNullUuids();
    }

    /**
     * Sends the given {@link Engagement} to be processed if the max retry limit has
     * not been exceeded.
     * 
     * @param event
     */
    @ConsumeEvent(value = EventType.RETRY_CREATE_EVENT_ADDRESS, blocking = true)
    void consumeRetryCreateEvent(RetriableEvent event) {
        retryEvent(event, () -> createOrUpdateEngagement(event, true));
    }

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

            // get current engagement from db
            Engagement persisted = engagementService.getByUuid(engagement.getUuid(), Optional.empty());

            // only resend if not updated
            if (null != persisted && persisted.getLastUpdate().equals(engagement.getLastUpdate())) {
                createOrUpdateEngagement(event, false);
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
        retryEvent(event, () -> deleteEngagement(event));
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

        Engagement engagement = event.getEngagement();

        try (Response response = gitApiClient.createOrUpdateEngagement(engagement, engagement.getLastUpdateByName(),
                engagement.getLastUpdateByEmail())) {

            if (isCreate) {
                Integer projectId = getProjectIdFromResponse(response);
                engagement.setProjectId(projectId);
                engagementService.setProjectId(engagement.getUuid(), projectId);
            }

        } catch (WebApplicationException e) {
            // rest call returned and 400 or above http code
            LOGGER.error("failed to create or update engagement with message '{}', engagement: {}", e.getMessage(),
                    engagement);

            String address = isCreate ? EventType.RETRY_CREATE_EVENT_ADDRESS : EventType.RETRY_UPDATE_EVENT_ADDRESS;
            event.incrementCurrentRetryCount();
            eventBus.sendAndForget(address, event);

        }

    }

    /**
     * Calls the delete API with the given {@link Engagement}.
     * 
     * If the API call produces an error code, the {@link RetriableEvent} will be
     * sent to the {@link EventBus} for reprocessing.
     * 
     * @param event
     */
    private void deleteEngagement(RetriableEvent event) {

        Engagement engagement = event.getEngagement();

        try {
            gitApiClient.deleteEngagement(engagement.getCustomerName(), engagement.getProjectName());
        } catch (WebApplicationException e) {

            LOGGER.error("failed to delete engagement with message {}, engagement: {}", e.getMessage(), engagement);
            event.incrementCurrentRetryCount();
            eventBus.sendAndForget(EventType.RETRY_DELETE_EVENT_ADDRESS, event);

        }

    }

    /**
     * Sets the project ID from the Location header on the {@link Engagement}.
     * 
     * @param engagement
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
     * Builds a {@link RetriableEvent} containing the given {@link Engagement}. If
     * the max retries was configured, it will be used.
     * 
     * @param engagement
     * @return
     */
    private RetriableEvent buildRetriableEvent(Engagement engagement) {

        RetriableEventBuilder builder = RetriableEvent.builder().engagement(engagement);

        if (null != eventMaxRetries) {
            builder.maxRetryCount(eventMaxRetries);
        }

        return builder.build();

    }

}