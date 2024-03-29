package com.redhat.labs.lodestar.service;

import java.util.Collections;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.EngagementUser;
import com.redhat.labs.lodestar.rest.client.ParticipantApiClient;

class ParticipantServiceTest {

    ParticipantService participantService;
    
    EngagementService engagementService;
    
    ParticipantApiClient participantClient;
    
    Set<EngagementUser> users;
    
    @BeforeEach
    void setup() {
        engagementService = Mockito.mock(EngagementService.class);
        participantClient = Mockito.mock(ParticipantApiClient.class);
        
        users = Collections.singleton(EngagementUser.builder().build());
        Engagement engagement = Engagement.builder().uuid("1").region("na").engagementUsers(users).build();
        Mockito.when(engagementService.getByUuid("1")).thenReturn(engagement);
        
        Mockito.when(participantClient.updateParticipants("1", "na", "b", "c", users)).thenReturn(Response.ok().build());
        Mockito.when(participantClient.updateParticipants("1", "na", "x", "c", users)).thenThrow(new WebApplicationException());
        Mockito.when(participantClient.updateParticipants("1", "na", "z", "c", users)).thenThrow(new RuntimeException());
        
        participantService = new ParticipantService();
        participantService.engagementService = engagementService;
        participantService.participantRestClient = participantClient;
    }
    
    @Test
    void testUpdateParticipants() {
        participantService.updateParticipants("1,b,c");
        Mockito.verify(participantClient).updateParticipants("1", "na", "b", "c", users);
        
        participantService.updateParticipants("1,x,c");
        Mockito.verify(participantClient).updateParticipants("1", "na", "x", "c", users);
        
        participantService.updateParticipants("1,z,c");
        Mockito.verify(participantClient).updateParticipants("1", "na", "z", "c", users);
    }
}
