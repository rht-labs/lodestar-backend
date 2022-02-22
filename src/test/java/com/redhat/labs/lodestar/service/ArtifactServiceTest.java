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
        Mockito.when(engagementService.getByUuid(Mockito.eq("uuid1"))).thenReturn(engagement);
        Mockito.when(engagementService.getByUuid(Mockito.eq("uuid2"))).thenThrow(new RuntimeException("test"));
        Mockito.when(engagementService.getByUuid(Mockito.eq("uuid3"))).thenThrow(new WebApplicationException("test", 500));
        Mockito.when(artifactClient.updateArtifacts("uuid1", "ma", artifacts, "Mitch", "mitch@mitch.com")).thenReturn(Response.ok().build());

        artifactService = new ArtifactService();
        artifactService.artifactRestClient = artifactClient;
        artifactService.engagementService = engagementService;
    }
    
    @Test
    void testSendUpdate() {
        String uuid = "uuid1";
        String name = "Mitch";
        String email = "mitch@mitch.com";
        Engagement e = Engagement.builder().region("na").uuid(uuid).artifacts(Collections.emptyList()).build();
        artifactService.update(e, name, email);
        
        Mockito.verify(artifactClient).updateArtifacts("uuid1", "na", artifacts, "mitch@mitch.com", "Mitch");
    }
    
    @Test
    void testSendUpdateRuntimeError() {

        String uuid = "uuid2";
        String name = "Mitch";
        String email = "mitch@mitch.com";
        Engagement e = Engagement.builder().region("na").uuid(uuid).artifacts(Collections.emptyList()).build();
        artifactService.update(e, name, email);
        
        Mockito.verify(artifactClient, Mockito.never()).updateArtifacts("uuid2", "na", artifacts, "Mitch", "mitch@mitch.com");
    }
     
    @Test
    void testSendUpdateWebError() {
        String uuid = "uuid3";
        String name = "Mitch";
        String email = "mitch@mitch.com";
        Engagement e = Engagement.builder().region("na").uuid(uuid).artifacts(Collections.emptyList()).build();
        artifactService.update(e, name, email);
        
        Mockito.verify(artifactClient, Mockito.never()).updateArtifacts("uuid3", "na", artifacts, "Mitch", "mitch@mitch.com");
    }

    @Test
    void testArtifactTypeCount() {

        artifactService.getTypesCount(Collections.emptyList());

        Mockito.verify(artifactClient).getTypesCount(Collections.emptyList());
    }
}
