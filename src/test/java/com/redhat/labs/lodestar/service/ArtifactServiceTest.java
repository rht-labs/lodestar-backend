package com.redhat.labs.lodestar.service;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.redhat.labs.lodestar.model.Artifact;
import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.filter.FilterOptions;
import com.redhat.labs.lodestar.rest.client.ArtifactApiClient;

class ArtifactServiceTest {

    ArtifactService artifactService;
    
    EngagementService engagementService;
    ArtifactApiClient artifactClient;
    List<Artifact> artifacts;
    
    @BeforeEach
    void setUp() {
        artifactClient = Mockito.mock(ArtifactApiClient.class);
        engagementService = Mockito.mock(EngagementService.class);
        
        artifacts = Collections.emptyList();
        Engagement engagement = Engagement.builder().artifacts(artifacts).build();
        Mockito.when(engagementService.getByUuid(Mockito.eq("uuid1"), Mockito.any(FilterOptions.class))).thenReturn(engagement);
        Mockito.when(engagementService.getByUuid(Mockito.eq("uuid2"), Mockito.any(FilterOptions.class))).thenThrow(new RuntimeException("test"));
        Mockito.when(engagementService.getByUuid(Mockito.eq("uuid3"), Mockito.any(FilterOptions.class))).thenThrow(new WebApplicationException("test", 500));
        Mockito.when(artifactClient.updateArtifacts("uuid1", artifacts, "Mitch", "mitch@mitch.com")).thenReturn(Response.ok().build());

        Mockito.when(artifactClient.refreshArtifacts()).thenThrow(new WebApplicationException());
        
        artifactService = new ArtifactService();
        artifactService.artifactRestClient = artifactClient;
        artifactService.engagementService = engagementService;
    }
    
    @Test
    void testSendUpdate() {
        String uuidNameEmail = "uuid1,Mitch,mitch@mitch.com"; 
        artifactService.sendUpdate(uuidNameEmail);
        
        Mockito.verify(artifactClient).updateArtifacts("uuid1", artifacts, "Mitch", "mitch@mitch.com");
    }
    
    @Test
    void testSendUpdateRuntimeError() {
        
        String uuidNameEmail = "uuid2,Mitch,mitch@mitch.com"; 
        artifactService.sendUpdate(uuidNameEmail);
        
        Mockito.verify(artifactClient, Mockito.never()).updateArtifacts("uuid2", artifacts, "Mitch", "mitch@mitch.com");
    }
     
    @Test
    void testSendUpdateWebError() {
        String uuidNameEmail = "uuid3,Mitch,mitch@mitch.com"; 
        artifactService.sendUpdate(uuidNameEmail);
        
        Mockito.verify(artifactClient, Mockito.never()).updateArtifacts("uuid3", artifacts, "Mitch", "mitch@mitch.com");
    }
    
    @Test
    void testRefresh() {
        artifactService.refesh("no");
        Mockito.verify(artifactClient).refreshArtifacts();
    }
}
