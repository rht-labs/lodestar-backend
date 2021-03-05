package com.redhat.labs.lodestar.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.gradle.internal.impldep.com.google.common.collect.Lists;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.Status;
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
    EventService eventService;

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
        Mockito.when(engagementService.getByUuid(Mockito.anyString(), Mockito.any()))
                .thenThrow(new WebApplicationException(404));

        eventBus.sendAndForget(EventType.UPDATE_ENGAGEMENT_EVENT_ADDRESS, e);

        Mockito.verify(gitApiClient, Mockito.timeout(1000).times(1)).createOrUpdateEngagement(e, "someone",
                "someone@example.com");

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

    @Test
    void testConsumeLoadDatabaseEvent() {

        // set engagements per page to 1
        eventService.engagementPerPage = 1;

        // list of engagements for page 1
        Engagement e1 = Engagement.builder().uuid("1111").customerName("c1").projectName("p1").build();
        List<Engagement> l1 = Lists.newArrayList(e1);
        // list of engagements for page 2
        Engagement e2 = Engagement.builder().uuid("2222").customerName("c2").projectName("p2").build();
        List<Engagement> l2 = Lists.newArrayList(e2);

        Response r1 = Response.ok(l1).header("x-last-page", 2).build();
        Response r2 = Response.ok(l2).header("x-last-page", 2).build();

        Status status = Status.builder().status("green").build();

        Mockito.when(gitApiClient.getEngagments(Mockito.eq(true), Mockito.anyInt(), Mockito.eq(1), Mockito.eq(false),
                Mockito.eq(false))).thenReturn(r1, r2);

        Mockito.when(engagementService.persistEngagementIfNotFound(Mockito.any())).thenReturn(true);
        Mockito.when(gitApiClient.getCommits(Mockito.anyString(), Mockito.anyString())).thenReturn(Lists.newArrayList());
        Mockito.when(gitApiClient.getStatus(Mockito.anyString(), Mockito.anyString())).thenReturn(status);

        // send load db event
        eventBus.sendAndForget(EventType.LOAD_DATABASE_EVENT_ADDRESS, EventType.LOAD_DATABASE_EVENT_ADDRESS);

        Mockito.verify(engagementService, Mockito.timeout(1000).times(2)).setCommits(Mockito.anyString(), Mockito.anyList());
        Mockito.verify(engagementService, Mockito.timeout(1000).times(2)).setStatus(Mockito.anyString(), Mockito.any(Status.class));

    }

    @Test
    void testConsumeDeleteAndReLoadDatabaseEvent() {

        // set engagements per page to 1
        eventService.engagementPerPage = 1;

        // list of engagements for page 1
        Engagement e1 = Engagement.builder().uuid("1111").customerName("c1").projectName("p1").build();
        List<Engagement> l1 = Lists.newArrayList(e1);
        // list of engagements for page 2
        Engagement e2 = Engagement.builder().uuid("2222").customerName("c2").projectName("p2").build();
        List<Engagement> l2 = Lists.newArrayList(e2);

        Response r1 = Response.ok(l1).header("x-last-page", 2).build();
        Response r2 = Response.ok(l2).header("x-last-page", 2).build();

        Status status = Status.builder().status("green").build();

        Mockito.when(gitApiClient.getEngagments(Mockito.eq(true), Mockito.anyInt(), Mockito.eq(1), Mockito.eq(false),
                Mockito.eq(false))).thenReturn(r1, r2);

        Mockito.when(engagementService.persistEngagementIfNotFound(Mockito.any())).thenReturn(true);
        Mockito.when(gitApiClient.getCommits(Mockito.anyString(), Mockito.anyString())).thenReturn(Lists.newArrayList());
        Mockito.when(gitApiClient.getStatus(Mockito.anyString(), Mockito.anyString())).thenReturn(status);

        // send load db event
        eventBus.sendAndForget(EventType.DELETE_AND_RELOAD_DATABASE_EVENT_ADDRESS, EventType.LOAD_DATABASE_EVENT_ADDRESS);

        Mockito.verify(engagementService, Mockito.timeout(1000)).deleteAll();
        Mockito.verify(engagementService, Mockito.timeout(1000).times(2)).setCommits(Mockito.anyString(), Mockito.anyList());
        Mockito.verify(engagementService, Mockito.timeout(1000).times(2)).setStatus(Mockito.anyString(), Mockito.any(Status.class));

    }

}