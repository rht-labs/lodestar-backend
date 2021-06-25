package com.redhat.labs.lodestar.resource;

import javax.annotation.security.PermitAll;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.lodestar.model.Hook;
import com.redhat.labs.lodestar.rest.client.LodeStarActivityApiClient;
import com.redhat.labs.lodestar.rest.client.LodeStarStatusApiClient;
import com.redhat.labs.lodestar.service.EngagementService;

@RequestScoped
@Path("/status")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Status", description = "Status stuff")
public class StatusResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatusResource.class);
    
    @ConfigProperty(name = "webhook.token")
    String statusToken;
    
    @ConfigProperty(name = "cleanup.token")
    String cleanupToken;
    
    @ConfigProperty(name = "engagement.file")
    String engagementFile;
    
    @Inject
    @RestClient
    LodeStarActivityApiClient activityApi;

    @Inject
    @RestClient
    LodeStarStatusApiClient statusClient;
    
    @Inject
    EngagementService engagementService;

    @POST
    @PermitAll
    @Path("/hook")
    @Tags({@Tag(ref="Status"), @Tag(ref="Activity")})
    @APIResponses(value = { 
            @APIResponse(responseCode = "401", description = "Invalid Gitlab Token"),
            @APIResponse(responseCode = "200", description = "Returns the hook given.") })
    @Operation(summary = "Entry point for update notifications")
    public Response statusUpdate(@HeaderParam(value = "x-gitlab-token") String gitLabToken, Hook hook) {
        
        if(!statusToken.equals(gitLabToken)) {
            LOGGER.error("Invalid status token used");
            return Response.status(Status.UNAUTHORIZED).build();
        }
        
        if(hook.didFileChange(engagementFile)) {
            activityApi.postHook(hook, gitLabToken);
        }
            
        LOGGER.debug("Hook for {}", hook.getProject().getPathWithNamespace());
        engagementService.updateStatusAndCommits(hook);

        return Response.ok(hook).build();
    }
    
    @POST
    @PermitAll
    @Path("/deleted")
    @APIResponses(value = {
            @APIResponse(responseCode = "401", description = "Invalid Token"),
            @APIResponse(responseCode = "200", description = "Returns the hook given.") })
    @Operation(summary = "Entry point for removing engagements")
    public Response removeEngagement(@HeaderParam(value = "x-notification-token") String secretTokenHeader, Hook hook) {
        
        if(!cleanupToken.equals(secretTokenHeader) || cleanupToken.equals("OFF")) {
            LOGGER.error("Invalid cleanup token used");
            return Response.status(Status.UNAUTHORIZED).build();
        }
        
        if(hook.wasProjectDeleted()) {
            LOGGER.debug("Remove engagement cust {} proj {}", hook.getCustomerName(), hook.getEngagementName());
            engagementService.deleteByCustomerAndProjectName(hook.getCustomerName(), hook.getEngagementName());
            return Response.status(204).build();
        }

        return Response.ok().build();
    }

    @GET
    @PermitAll
    @APIResponses(value = { 
            @APIResponse(responseCode = "200", description = "Component Status has been returned.") })
    @Operation(summary = "Returns status of all configured components.")
    public Response getComponentStatus() {
        return statusClient.getComponentStatus();
    }

}
