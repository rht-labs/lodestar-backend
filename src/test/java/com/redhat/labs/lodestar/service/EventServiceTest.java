package com.redhat.labs.lodestar.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.event.EventType;
import com.redhat.labs.lodestar.utils.IntegrationTestHelper;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.vertx.mutiny.core.eventbus.EventBus;

@QuarkusTest
@Tag("integration")
class EventServiceTest extends IntegrationTestHelper {

    @InjectMock
    EngagementService engagementService;

    @Inject
    EventBus eventBus;

    @Test
    void testConsumeCreateEngagementEventSuccess() {

        Mockito.when(gitApiClient.createOrUpdateEngagement(Mockito.any(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Response.status(201).header("Location", "some/path/to/id/5678").build());

        Engagement e = Engagement.builder().uuid("1234").customerName("c1").projectName("p1")
                .lastUpdateByName("someone").lastUpdateByEmail("someone@example.com").build();
        eventBus.sendAndForget(EventType.CREATE_ENGAGEMENT_EVENT_ADDRESS, e);

        Mockito.verify(gitApiClient, Mockito.timeout(1000).times(1)).createOrUpdateEngagement(e, "someone",
                "someone@example.com");
        Mockito.verify(engagementService, Mockito.timeout(1000).times(1)).setProjectId("1234", 5678);

    }

    @Test
    void testConsumeCreateEngagementEventRetry() {

        Response created = Response.status(201).header("Location", "some/path/to/id/5678").build();

        Mockito.when(gitApiClient.createOrUpdateEngagement(Mockito.any(), Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new WebApplicationException(500)).thenReturn(created);

        Engagement e = Engagement.builder().uuid("1234").customerName("c1").projectName("p1")
                .lastUpdateByName("someone").lastUpdateByEmail("someone@example.com").build();
        eventBus.sendAndForget(EventType.CREATE_ENGAGEMENT_EVENT_ADDRESS, e);

        Mockito.verify(gitApiClient, Mockito.timeout(2000).times(2)).createOrUpdateEngagement(e, "someone",
                "someone@example.com");
        Mockito.verify(engagementService, Mockito.timeout(2000).times(1)).setProjectId("1234", 5678);

    }

    @Test
    void testConsumeCreateEngagementEventMaxRetriesReached() {

        Mockito.when(gitApiClient.createOrUpdateEngagement(Mockito.any(), Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new WebApplicationException(500));

        Engagement e = Engagement.builder().uuid("1234").customerName("c1").projectName("p1")
                .lastUpdateByName("someone").lastUpdateByEmail("someone@example.com").build();
        eventBus.sendAndForget(EventType.CREATE_ENGAGEMENT_EVENT_ADDRESS, e);

        Mockito.verify(gitApiClient, Mockito.timeout(2000).times(2)).createOrUpdateEngagement(e, "someone",
                "someone@example.com");
        Mockito.verify(engagementService, Mockito.timeout(2000).times(0)).setProjectId("1234", 5678);

    }

    @Test
    void testConsumeUpdateEngagementEventSuccess() {

        Mockito.when(gitApiClient.createOrUpdateEngagement(Mockito.any(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Response.status(201).header("Location", "some/path/to/id/5678").build());

        Engagement e = Engagement.builder().uuid("1234").customerName("c1").projectName("p1")
                .lastUpdateByName("someone").lastUpdateByEmail("someone@example.com").build();
        eventBus.sendAndForget(EventType.UPDATE_ENGAGEMENT_EVENT_ADDRESS, e);

        Mockito.verify(gitApiClient, Mockito.timeout(1000).times(1)).createOrUpdateEngagement(e, "someone",
                "someone@example.com");
        Mockito.verify(engagementService, Mockito.timeout(1000).times(0)).setProjectId("1234", 5678);

    }

    @Test
    void testConsumeUpdateEngagementEventRetry() {

        Response created = Response.status(201).header("Location", "some/path/to/id/5678").build();
        Engagement e = Engagement.builder().uuid("1234").customerName("c1").projectName("p1")
                .lastUpdateByName("someone").lastUpdateByEmail("someone@example.com")
                .lastUpdate(ZonedDateTime.now(ZoneId.of("Z")).toString()).build();

        Mockito.when(gitApiClient.createOrUpdateEngagement(Mockito.any(), Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new WebApplicationException(500)).thenReturn(created);
        Mockito.when(engagementService.getByUuid(Mockito.anyString(), Mockito.any())).thenReturn(e);

        eventBus.sendAndForget(EventType.UPDATE_ENGAGEMENT_EVENT_ADDRESS, e);

        Mockito.verify(gitApiClient, Mockito.timeout(5000).times(2)).createOrUpdateEngagement(e, "someone",
                "someone@example.com");
        Mockito.verify(engagementService, Mockito.timeout(2000).times(0)).setProjectId("1234", 5678);

    }

    @Test
    void testConsumeUpdateEngagementEventRetryAlreadyUpdated() {

        Response created = Response.status(201).header("Location", "some/path/to/id/5678").build();
        Engagement e = Engagement.builder().uuid("1234").customerName("c1").projectName("p1")
                .lastUpdateByName("someone").lastUpdateByEmail("someone@example.com")
                .lastUpdate(ZonedDateTime.now(ZoneId.of("Z")).toString()).build();
        Engagement updated = e.toBuilder().lastUpdate(ZonedDateTime.now(ZoneId.of("Z")).toString()).build();

        Mockito.when(gitApiClient.createOrUpdateEngagement(Mockito.any(), Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new WebApplicationException(500)).thenReturn(created);
        Mockito.when(engagementService.getByUuid(Mockito.anyString(), Mockito.any())).thenReturn(updated);

        eventBus.sendAndForget(EventType.UPDATE_ENGAGEMENT_EVENT_ADDRESS, e);

        Mockito.verify(gitApiClient, Mockito.timeout(1000).times(1)).createOrUpdateEngagement(e, "someone",
                "someone@example.com");
        Mockito.verify(engagementService, Mockito.timeout(1000).times(0)).setProjectId("1234", 5678);

    }

    @Test
    void testConsumeUpdateEngagementEventRetryEngagementRemoved() {

        Response created = Response.status(201).header("Location", "some/path/to/id/5678").build();
        Engagement e = Engagement.builder().uuid("1234").customerName("c1").projectName("p1")
                .lastUpdateByName("someone").lastUpdateByEmail("someone@example.com")
                .lastUpdate(ZonedDateTime.now(ZoneId.of("Z")).toString()).build();

        Mockito.when(gitApiClient.createOrUpdateEngagement(Mockito.any(), Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new WebApplicationException(500)).thenReturn(created);
        Mockito.when(engagementService.getByUuid(Mockito.anyString(), Mockito.any())).thenThrow(new WebApplicationException(404));

        eventBus.sendAndForget(EventType.UPDATE_ENGAGEMENT_EVENT_ADDRESS, e);

        Mockito.verify(gitApiClient, Mockito.timeout(1000).times(1)).createOrUpdateEngagement(e, "someone",
                "someone@example.com");
//        Mockito.verify(engagementService, Mockito.timeout(1000).times(0)).setProjectId("1234", 5678);

    }

    @Test
    void testConsumeUpdateEngagementEventMaxRetriesReached() {

        Mockito.when(gitApiClient.createOrUpdateEngagement(Mockito.any(), Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new WebApplicationException(500));

        Engagement e = Engagement.builder().uuid("1234").customerName("c1").projectName("p1")
                .lastUpdateByName("someone").lastUpdateByEmail("someone@example.com").build();
        eventBus.sendAndForget(EventType.CREATE_ENGAGEMENT_EVENT_ADDRESS, e);

        Mockito.verify(gitApiClient, Mockito.timeout(2000).times(2)).createOrUpdateEngagement(e, "someone",
                "someone@example.com");
        Mockito.verify(engagementService, Mockito.timeout(2000).times(0)).setProjectId("1234", 5678);

    }

    @Test
    void testConsumeDeleteEngagementEventSuccess() {

        Engagement e = Engagement.builder().uuid("1234").customerName("c1").projectName("p1")
                .lastUpdateByName("someone").lastUpdateByEmail("someone@example.com").build();
        eventBus.sendAndForget(EventType.DELETE_ENGAGEMENT_EVENT_ADDRESS, e);

        Mockito.verify(gitApiClient, Mockito.timeout(1000).times(1)).deleteEngagement("c1", "p1");

    }

    @Test
    void testConsumeDeleteEngagementEventMaxRetriesReached() {

        Engagement e = Engagement.builder().uuid("1234").customerName("c1").projectName("p1")
                .lastUpdateByName("someone").lastUpdateByEmail("someone@example.com").build();

        Mockito.doThrow(new WebApplicationException(500)).when(gitApiClient).deleteEngagement("c1", "p1");

        eventBus.sendAndForget(EventType.DELETE_ENGAGEMENT_EVENT_ADDRESS, e);

        Mockito.verify(gitApiClient, Mockito.timeout(2000).times(2)).deleteEngagement("c1", "p1");

    }

}