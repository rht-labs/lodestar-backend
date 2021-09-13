package com.redhat.labs.lodestar.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.EngagementUser;
import com.redhat.labs.lodestar.model.event.EventType;
import com.redhat.labs.lodestar.model.filter.FilterOptions;
import com.redhat.labs.lodestar.rest.client.ParticipantApiClient;

import io.quarkus.vertx.ConsumeEvent;

@ApplicationScoped
public class ParticipantService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantService.class);
            
    @Inject
    @RestClient
    ParticipantApiClient participantRestClient;
    
    @Inject
    EngagementService engagementService;
    
    public List<EngagementUser> getParticipantsForEngagement(String engagementUuid) {
        return participantRestClient.getParticipantsForEngagement(engagementUuid);
    }
    
    public Response getParticipants(int page, int pageSize) {
        return getParticipants(null, page, pageSize);
    }
    
    public Response getParticipants(Set<String> engagementUuids, Integer page, Integer pageSize) {
        return participantRestClient.getParticipants(engagementUuids, page, pageSize);
    }
    
    public Map<String, Long> getEnabledParticipants(List<String> region) {
        return participantRestClient.getEnabledParticipants(region);
    }
    
    public Map<String,Map<String, Long>> getEnabledParticipantsAllRegions() {
        return participantRestClient.getEnabledParticipantsAllRegions();
    }
    
    /**
     * Sync - directly called api from the FE
     * @param engagementUuid the engagement uuid to update
     * @param authorName the name of the updater
     * @param authorEmail the email of the user
     * @param participants the full list of participants for an engagement
     * @return A response value indicating success (200) or failure (anything else)
     */
    public Response updateParticipants(String engagementUuid, String authorName, String authorEmail, Set<EngagementUser> participants) {
        
        Engagement engagement = engagementService.getByUuid(engagementUuid);
        return participantRestClient.updateParticipants(engagementUuid, engagement.getRegion(), authorName, authorEmail, participants);
    }

    public List<EngagementUser> updateParticipantsAndReload(String engagementUuid, String authorName, String authorEmail, Set<EngagementUser> participants) {

        updateParticipants(engagementUuid, authorName, authorEmail, participants);
        return getParticipantsForEngagement(engagementUuid);
    }
    
    /**
     * Async
     * @param message Comma separated string of uuid, name, email
     */
    @ConsumeEvent(value = EventType.UPDATE_PARTICIPANTS_EVENT_ADDESS)
    public void updateParticipants(String message) {
        String[] uuidNameEmail = message.split(",");
        
        Engagement engagement = engagementService.getByUuid(uuidNameEmail[0]);
        
        try {
            participantRestClient.updateParticipants(engagement.getUuid(), engagement.getRegion(),uuidNameEmail[1], uuidNameEmail[2], engagement.getEngagementUsers());
            LOGGER.debug("Updated participants for engagement {}", engagement.getUuid());
        } catch (WebApplicationException wae) {
            LOGGER.error("Failed to update participants for engagement {} {}", wae.getResponse().getStatus(), message);
        } catch (RuntimeException wae) {
            LOGGER.error("Failed to update participants for engagement {}", message, wae);
        }
        
    }
    
    @ConsumeEvent(value = EventType.RELOAD_PARTICIPANTS_EVENT_ADDRESS, blocking = true)
    public void refesh(String message) {
        try {
            LOGGER.debug("refresh {}", message);
            Response response = participantRestClient.refreshParticipants();
            LOGGER.debug("refresh {} completed. Participant count is {} ", message, response.getHeaderString("x-total-participants"));
        } catch (WebApplicationException wae) { //without catching this it will fail silently
            LOGGER.error("Failed to refresh participants {}", wae.getResponse(), wae);
        }
    }
}
