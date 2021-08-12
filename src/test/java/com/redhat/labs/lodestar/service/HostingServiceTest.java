package com.redhat.labs.lodestar.service;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.HostingEnvironment;
import com.redhat.labs.lodestar.model.filter.FilterOptions;
import com.redhat.labs.lodestar.rest.client.HostingApiClient;

class HostingServiceTest {
    
    HostingService hostingService;
    
    HostingApiClient client;
    
    EngagementService engagementService;
    
    List<HostingEnvironment> hostingEnvironments;
    
    @BeforeEach
    void setup() {
        engagementService = Mockito.mock(EngagementService.class);
        client = Mockito.mock(HostingApiClient.class);
        
        hostingService = new HostingService();
        hostingService.hostingApiClient = client;
        hostingService.engagementService = engagementService;
        
        hostingEnvironments = Collections.singletonList(HostingEnvironment.builder().engagementUuid("uuid1").build());
        
        Engagement engagement = Engagement.builder().uuid("uuid1").region("na").hostingEnvironments(hostingEnvironments).build();
        Mockito.when(engagementService.getByUuid(Mockito.eq("uuid1"), Mockito.any(FilterOptions.class))).thenReturn(engagement);
        
        Mockito.when(client.updateHostingEnvironments("uuid1", "wae@email.com", "name", hostingEnvironments)).thenThrow(new WebApplicationException());
        Mockito.when(client.updateHostingEnvironments("uuid1", "re@email.com", "name", hostingEnvironments)).thenThrow(new RuntimeException());
        
        Mockito.when(client.refreshHostingEnvironments()).thenThrow(new WebApplicationException());
    }

    @Test
    void testUpdate() {
        hostingService.updateHostingEnvironments("uuid1,email@email.com,name");
        Mockito.verify(client).updateHostingEnvironments("uuid1", "email@email.com", "name", hostingEnvironments);
        
        hostingService.updateHostingEnvironments("uuid1,wae@email.com,name");
        Mockito.verify(client).updateHostingEnvironments("uuid1", "wae@email.com", "name", hostingEnvironments);
        
        hostingService.updateHostingEnvironments("uuid1,re@email.com,name");
        Mockito.verify(client).updateHostingEnvironments("uuid1", "re@email.com", "name", hostingEnvironments);
    }
    
    @Test
    void testRefresh() {
        hostingService.refesh("no");
        Mockito.verify(client).refreshHostingEnvironments();
    }
}
