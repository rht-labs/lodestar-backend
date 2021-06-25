package com.redhat.labs.lodestar.resource;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.lodestar.model.event.EventType;
import com.redhat.labs.lodestar.rest.client.LodeStarActivityApiClient;

import io.vertx.mutiny.core.eventbus.EventBus;

@RequestScoped
@Path("engagements/activity")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Activity", description = "Activity History for Engagements")
public class ActivityResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityResource.class);

    @Inject
    @RestClient
    LodeStarActivityApiClient activityClient;
    
    @Inject
    EventBus eventBus;
    
    @GET
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "400", description = "Pay attention to your paging."),
            @APIResponse(responseCode = "200", description = "Activity list.") })
    @Operation(summary = "Returns all activity for an engagement by uuid.")
    @Path("/uuid/{uuid}")
    public Response fetchActivity(@PathParam(value = "uuid") String uuid, @QueryParam("page") Integer page, @QueryParam("pageSize") Integer pageSize) {
        LOGGER.trace("uuid {}", uuid);
        
        if(page == null || pageSize == null) {
            return activityClient.getActivity(uuid);
        } else if(page < 0 || pageSize < 1) {
            return Response.status(Status.BAD_REQUEST).entity("Invalid pagination").build();
        } 
            
        System.out.println("callled fefe");
        return activityClient.getPaginatedActivity(uuid, page, pageSize);
    }
    
    @PUT
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Activity list.") })
    @Operation(summary = "Refresh all activity across all engagements.")
    @Path("refresh")
    public Response refresh() {
        System.out.println("callled refresh");
        eventBus.sendAndForget(EventType.RELOAD_ACTIVITY_EVENT_ADDRESS, EventType.RELOAD_ACTIVITY_EVENT_ADDRESS);
        return Response.accepted().build(); 
    }
}
