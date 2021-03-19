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

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.lodestar.model.Commit;
import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.EngagementUser;
import com.redhat.labs.lodestar.model.Status;
import com.redhat.labs.lodestar.model.event.EventType;
import com.redhat.labs.lodestar.model.event.RetriableEvent;
import com.redhat.labs.lodestar.model.event.RetriableEvent.RetriableEventBuilder;
import com.redhat.labs.lodestar.model.filter.FilterOptions;
import com.redhat.labs.lodestar.rest.client.LodeStarGitLabAPIService;

import io.quarkus.vertx.ConsumeEvent;
import io.vertx.mutiny.core.eventbus.EventBus;

public class EventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventService.class);

    private static final String LAST_PAGE_HEADER = "x-last-page";

    @ConfigProperty(name = "event.max.retries", defaultValue = "-1")
    Integer eventMaxRetries;

    @ConfigProperty(name = "event.retry.delay.factor", defaultValue = "2")
    Integer eventRetryDelayFactor;

    @ConfigProperty(name = "event.retry.max.delay", defaultValue = "60")
    Integer eventRetryMaxDelay;

    @ConfigProperty(name = "get.engagement.per.page", defaultValue = "20")
    Integer engagementPerPage;

    @Inject
    @RestClient
    LodeStarGitLabAPIService gitApiClient;

    @Inject
    EngagementService engagementService;

    @Inject
    EventBus eventBus;

    @Inject
    Jsonb jsonb;

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

            try {

                // get current engagement from db
                Engagement persisted = engagementService.getByUuid(engagement.getUuid(), new FilterOptions());

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

    /*
     * Retrieves the first page of {@link Engagement}s from the Git API. Then,
     * creates a GET_PAGE_OF_ENGAGEMENTS_EVENT for each remaining page of results.
     */
    private void getEngagements() {

        // get first page of engagements
        Response response = getPageOfEngagements(1);

        // process first page of engagements
        // convert to json for event bus
        String json = jsonb.toJson(getEngagementsFromResponse(response));

        // send engagements for processing
        eventBus.sendAndForget(EventType.PERSIST_ENGAGEMENT_LIST_EVENT_ADDRESS, json);

        // get total number of pages from response
        Integer totalPages = getNumberOfPages(response);
        LOGGER.trace("found {} total pages.", totalPages);

        // close response
        response.close();

        if (totalPages > 1) {

            IntStream.range(2, totalPages + 1)
                    .forEach(i -> eventBus.sendAndForget(EventType.GET_PAGE_OF_ENGAGEMENTS_EVENT_ADDRESS, i));

        }

    }

    /**
     * Returns the Response from the Git API for the provided page number.
     * 
     * @param page
     * @return
     */
    private Response getPageOfEngagements(Integer page) {

        LOGGER.trace("getting page {} of engagements from git api.", page);

        // get page of engagements from git api
        return gitApiClient.getEngagments(true, page, engagementPerPage, false, false);

    }

    /**
     * Gets the engagement from the Git API and sends event to delete and reinsert.
     * 
     * @param engagement
     */
    @ConsumeEvent(value = EventType.DELETE_AND_RELOAD_ENGAGEMENT_EVENT_ADDRESS)
    void consumeDeleteAndReLoadEngagementEvent(String projectId) {

        if (null != projectId) {

            // get engagement by project id from git api
            Engagement found = gitApiClient.getEngagementByNamespace(projectId);

            if(null != found) {
                // send event to delete existing engagement from database
                eventBus.sendAndForget(EventType.DELETE_ENGAGEMENT_FROM_DATABASE_EVENT_ADDRESS, found);
            }

        }

    }

    /**
     * Removes the {@link Engagement} based on UUID and sends event to reinsert.
     * 
     * @param engagement
     */
    @ConsumeEvent(value = EventType.DELETE_ENGAGEMENT_FROM_DATABASE_EVENT_ADDRESS)
    void consumeDeleteEngagementFromDatabaseEvent(Engagement engagement) {

        try {
            // remove existing engagement from database
            engagementService.deleteByUuid(engagement.getUuid());
        } catch(WebApplicationException wae) {
            LOGGER.info("no engagement found in database with id {}", engagement.getUuid());
        }

        // send event to insert engagement
        eventBus.sendAndForget(EventType.PERSIST_ENGAGEMENT_EVENT_ADDRESS, engagement);

    }

    /**
     * Starts the process of inserting any {@link Engagement}s in Git that are not
     * in the database. Existing {@link Engagement}s will not be updated.
     * 
     * @param event
     */
    @ConsumeEvent(value = EventType.LOAD_DATABASE_EVENT_ADDRESS, blocking = true)
    void consumeLoadDatabaseEvent(String event) {

        // load all engagements from gitlab
        getEngagements();

    }

    /**
     * Starts the process of inserting all {@link Engagement}s in Git. All
     * {@link Engagement}s are removed from the database before inserting.
     * 
     * @param event
     */
    @ConsumeEvent(value = EventType.DELETE_AND_RELOAD_DATABASE_EVENT_ADDRESS, blocking = true)
    void consumeDeleteAndReLoadDatabaseEvent(String event) {

        // delete all
        engagementService.deleteAll();
        LOGGER.debug("cleared engagements from database.");

        // reload all engagements from gitlab
        getEngagements();

    }

    /**
     * Get all {@link Engagement}s for the specified page and send event to process
     * each one.
     * 
     * @param pageNumber
     */
    @ConsumeEvent(value = EventType.GET_PAGE_OF_ENGAGEMENTS_EVENT_ADDRESS, blocking = true)
    void consumeGetPageEvent(Integer pageNumber) {

        // get given page of engagements
        Response response = getPageOfEngagements(pageNumber);
        // pull engagements list from response
        List<Engagement> engagements = getEngagementsFromResponse(response);
        // close after using
        response.close();

        // convert to json for event bus
        String json = jsonb.toJson(engagements);

        // send engagements for processing
        eventBus.sendAndForget(EventType.PERSIST_ENGAGEMENT_LIST_EVENT_ADDRESS, json);

    }

    /**
     * Creates a PERSIST_ENGAGEMENT_EVENT for each {@link Engagement} in the
     * provided {@link List}.
     * 
     * @param engagements
     */
    @ConsumeEvent(value = EventType.PERSIST_ENGAGEMENT_LIST_EVENT_ADDRESS)
    void consumeProcessEngagementList(String engagementsJson) {

        // unmarshal
        Engagement[] engagements = jsonb.fromJson(engagementsJson, Engagement[].class);
        // create event for each engagement
        Arrays.stream(engagements).forEach(e -> eventBus.sendAndForget(EventType.PERSIST_ENGAGEMENT_EVENT_ADDRESS, e));

    }

    /**
     * Persists the {@link Engagement} in the database if it does not already exist.
     * 
     * @param engagement
     */
    @ConsumeEvent(value = EventType.PERSIST_ENGAGEMENT_EVENT_ADDRESS)
    void consumeInsertIfMissingEvent(Engagement engagement) {

        if (engagementService.persistEngagementIfNotFound(engagement)) {
            eventBus.sendAndForget(EventType.UPDATE_STATUS_EVENT_ADDRESS, engagement);
            eventBus.sendAndForget(EventType.UPDATE_COMMITS_EVENT_ADDRESS, engagement);
        }

    }

    /**
     * Retrieves the {@link List} of {@link Commit}s for the given
     * {@link Engagement} from the Git API and then updates the database.
     * 
     * @param engagement
     */
    @ConsumeEvent(value = EventType.UPDATE_COMMITS_EVENT_ADDRESS, blocking = true)
    void consumeUpdateCommitsEvent(Engagement engagement) {

        List<Commit> commits = gitApiClient.getCommits(engagement.getCustomerName(), engagement.getProjectName());
        engagementService.setCommits(engagement.getUuid(), commits);

    }

    /**
     * Retrieves the {@link Status} for the given {@link Engagement} from the Git
     * API and then updates the database.
     * 
     * @param engagement
     */
    @ConsumeEvent(value = EventType.UPDATE_STATUS_EVENT_ADDRESS, blocking = true)
    void consumeUpdateStatusEvent(Engagement engagement) {

        try {
            Status status = gitApiClient.getStatus(engagement.getCustomerName(), engagement.getProjectName());
            engagementService.setStatus(engagement.getUuid(), status);
        } catch (WebApplicationException wae) {
            LOGGER.trace("no status found for engagement {}:{}:{}", engagement.getUuid(), engagement.getCustomerName(),
                    engagement.getProjectName());
        }

    }

}