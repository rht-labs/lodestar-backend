package com.redhat.labs.lodestar.resource;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;

import com.redhat.labs.lodestar.model.event.BackendEvent;

import io.vertx.mutiny.core.eventbus.EventBus;

@RequestScoped
@Path("/engagements")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GitSyncResource {

    @Inject
    EventBus eventBus;

    @Inject
    JsonWebToken jwt;

    @PUT
    @Path("/process/modified")
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = {
            @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "All modified engagement records persisted in git.")})
    @Operation(summary = "Sends all modified engagements to git to be stored.")
    public Response push() {

        // send time elapsed event to start push to git from db
        BackendEvent event = BackendEvent.createPushToGitRequestedEvent();
        eventBus.sendAndForget(event.getEventType().getEventBusAddress(), event);
        return Response.ok().build();

    }

}
