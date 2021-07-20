package com.redhat.labs.lodestar.resource;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.redhat.labs.lodestar.model.event.EventType;
import com.redhat.labs.lodestar.rest.client.ActivityApiClient;
import com.redhat.labs.lodestar.service.ArtifactService;
import com.redhat.labs.lodestar.service.EngagementService;
import com.redhat.labs.lodestar.service.ParticipantService;

import io.vertx.mutiny.core.eventbus.EventBus;

@RequestScoped
@Path("/engagements/refresh")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityScheme(securitySchemeName = "jwt", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@Tag(name = "Refresh", description = "Refresh data for all services")
public class RefreshResource {

    @Inject
    EngagementService engagementService;

    @Inject
    ParticipantService participantService;

    @Inject
    ArtifactService artifactService;

    @Inject
    @RestClient
    ActivityApiClient activityClient;

    @Inject
    EventBus eventBus;

    @PUT
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "400", description = "No refresh data set selected. Options are engagements, artifacts, participants and activity. You can mix and match"),
            @APIResponse(responseCode = "404", description = "UUID provided, but no engagement found in database."),
            @APIResponse(responseCode = "202", description = "The request was accepted and will be processed.") })
    @Operation(summary = "Refreshes database with data in git, purging first if the query paramater set to true.")
    @Counted(name = "engagement-put-refresh-counted")
    @Timed(name = "engagement-put-refresh-timer", unit = MetricUnits.MILLISECONDS)
    public Response refresh(
            @Parameter(description = "When set deletes engagements first.") @QueryParam("purgeFirst") Boolean purgeFirst,
            @Parameter(description = "Refresh engagement with uuid") @QueryParam("uuid") String uuid,
            @Parameter(description = "Refresh engagement with project id") @QueryParam("projectId") String projectId,
            @Parameter(description = "Refresh artifacts") @QueryParam("artifacts") boolean refreshArtifacts,
            @Parameter(description = "Refresh participants") @QueryParam("participants") boolean refreshParticipants,
            @Parameter(description = "Refresh activity") @QueryParam("activity") boolean refreshActivity,
            @Parameter(description = "Refresh engagements") @QueryParam("engagements") boolean refreshEngagements) {

        boolean didPickSomething = false;

        if (refreshActivity) {
            eventBus.sendAndForget(EventType.RELOAD_ACTIVITY_EVENT_ADDRESS, EventType.RELOAD_ACTIVITY_EVENT_ADDRESS);
            didPickSomething = true;
        }

        if (refreshParticipants) {
            eventBus.sendAndForget(EventType.RELOAD_PARTICIPANTS_EVENT_ADDRESS,
                    EventType.RELOAD_PARTICIPANTS_EVENT_ADDRESS);
            didPickSomething = true;
        }

        if (refreshArtifacts) {
            eventBus.sendAndForget(EventType.RELOAD_ARTIFACTS_EVENT_ADDRESS, EventType.RELOAD_ARTIFACTS_EVENT_ADDRESS);
            didPickSomething = true;
        }

        if (refreshEngagements) {
            engagementService.syncGitToDatabase(Boolean.TRUE.equals(purgeFirst), uuid, projectId);
            didPickSomething = true;
        }

        if (didPickSomething) {
            return Response.accepted().build();
        }

        return Response.status(400).entity("{ \"message\" : \"No refresh source was selected\" }").build();

    }

}
