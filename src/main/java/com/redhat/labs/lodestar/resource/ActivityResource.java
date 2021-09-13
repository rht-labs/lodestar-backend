package com.redhat.labs.lodestar.resource;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.redhat.labs.lodestar.model.*;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.lodestar.rest.client.ActivityApiClient;

import io.vertx.mutiny.core.eventbus.EventBus;

import java.util.*;

@RequestScoped
@Path("engagements/activity")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Activity", description = "Activity History for Engagements")
public class ActivityResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityResource.class);

    @Inject
    @RestClient
    ActivityApiClient activityClient;
    
    @Inject
    EventBus eventBus;
    
    @GET
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "400", description = "Pay attention to your paging."),
            @APIResponse(responseCode = "200", description = "Activity list sorted by time desc") })
    @Operation(summary = "Returns all activity for an engagement by uuid.")
    public Response fetchActivity(@QueryParam("page") int page, @QueryParam("pageSize") int pageSize) {
        if(page < 0 || pageSize < 1) {
            return Response.status(Status.BAD_REQUEST).entity("{ \"error\": \"Invalid pagination\"}").build();
        }
        
        return activityClient.getPaginatedActivity(page, pageSize);
    }
    
    @GET
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "400", description = "Pay attention to your paging."),
            @APIResponse(responseCode = "200", description = "Activity list.") })
    @Operation(summary = "Returns all activity for an engagement by uuid.")
    @Path("/uuid/{uuid}")
    public Response fetchActivityByUuid(@PathParam(value = "uuid") String uuid, @QueryParam("page") Integer page, @QueryParam("pageSize") Integer pageSize) {
        LOGGER.trace("uuid {}", uuid);
        
        if(page == null || pageSize == null) {
            List<Commit> activity = activityClient.getActivityForUuid(uuid);
            return Response.ok(activity).header("x-total-activity", activity.size()).build();
        } else if(page < 0 || pageSize < 1) {
            return Response.status(Status.BAD_REQUEST).entity("{ \"error\": \"Invalid pagination\"}").build();
        } 
            
        return activityClient.getPaginatedActivityForUuid(uuid, page, pageSize);
    }

}
