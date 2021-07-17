package com.redhat.labs.lodestar.service;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

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
    
    public List<EngagementUser> getParticiipantsForEngagement(String engagementUuid) {
        return participantRestClient.getParticipantsForEngagement(engagementUuid);
    }
    
    @ConsumeEvent(value = EventType.UPDATE_PARTICIPANTS_EVENT_ADDESS)
    public void updateParticipants(String message) {
        String[] uuidNameEmail = message.split(",");
        
        Engagement engagement = engagementService.getByUuid(uuidNameEmail[0], new FilterOptions());
        
        try {
            participantRestClient.updateParticipants(uuidNameEmail[0], uuidNameEmail[1], uuidNameEmail[2], engagement.getEngagementUsers());
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
            participantRestClient.refreshParticipants();
            LOGGER.debug("refresh {} completed", message);
        } catch (WebApplicationException wae) { //without catching this it will fail silently
            LOGGER.error("Failed to refresh participants {}", wae.getResponse(), wae);
        }
    }
}
