package com.redhat.labs.lodestar.service;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.event.EventType;
import com.redhat.labs.lodestar.rest.client.LodeStarGitLabAPIService;

import io.quarkus.vertx.ConsumeEvent;

public class EventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventService.class);

    @Inject
    @RestClient
    LodeStarGitLabAPIService gitApiClient;

    @Inject
    EngagementService engagementService;

    // create engagement
    @ConsumeEvent(value = EventType.CREATE_ENGAGEMENT_EVENT_ADDRESS, blocking = true)
    void consumeCreateEngagementEvent(Engagement engagement) {

        createOrUpdateEngagement(engagement, true);
        if (null != engagement.getProjectId()) {
            engagementService.setProjectId(engagement.getUuid(), engagement.getProjectId());
        }

    }

    // update engagement
    @ConsumeEvent(value = EventType.UPDATE_ENGAGEMENT_EVENT_ADDRESS, blocking = true)
    void consumeUpdateEngagementEvent(Engagement engagement) {
        createOrUpdateEngagement(engagement, false);
    }

    // delete engagement
    @ConsumeEvent(EventType.DELETE_ENGAGEMENT_EVENT_ADDRESS)
    void consumeDeleteEngagementEvent(Engagement engagement) {
        gitApiClient.deleteEngagement(engagement.getCustomerName(), engagement.getProjectName());
    }

    // db refresh
    @ConsumeEvent(value = EventType.REFRESH_DATABASE_EVENT_ADDRESS, blocking = true)
    void consumeRefreshDatabaseEvent(String event) {
        engagementService.syncGitToDatabase(false);
    }

    // uuid setting
    @ConsumeEvent(value = EventType.SET_UUID_EVENT_ADDRESS, blocking = true)
    void setUuidEvent(String event) {
        engagementService.setNullUuids();
    }

    private void createOrUpdateEngagement(Engagement engagement, boolean isCreate) {

        try (Response response = gitApiClient.createOrUpdateEngagement(engagement, engagement.getLastUpdateByName(),
                engagement.getLastUpdateByEmail())) {

            if (isCreate) {
                Integer projectId = getProjectIdFromResponse(response);
                engagement.setProjectId(projectId);
            }

        } catch (WebApplicationException e) {
            // rest call returned and 400 or above http code
            LOGGER.error(
                    "failed to create or update engagement with message '{}', please check to see data needs to be refreshed from git. engagement: {}",
                    e.getMessage(), engagement);

            // TODO: send event to retry - probably need a counter
            // should probably only retry if updated timestamp matches

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

}