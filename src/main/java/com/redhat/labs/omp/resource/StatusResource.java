package com.redhat.labs.omp.resource;

import javax.annotation.security.PermitAll;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
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
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.omp.model.Hook;
import com.redhat.labs.omp.rest.client.OMPGitLabAPIService;
import com.redhat.labs.omp.service.EngagementService;

@RequestScoped
@Path("/status")
@Produces(MediaType.APPLICATION_JSON)
public class StatusResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatusResource.class);
    
    @ConfigProperty(name = "webhook.token")
    String token;
    
    @ConfigProperty(name = "status.file")
    String statusFile;
    
    @Inject
    @RestClient
    OMPGitLabAPIService gitApi;
    
    @Inject
    EngagementService engagementService;

    @POST
    @PermitAll
    @Path("/hook")
    @APIResponses(value = { 
            @APIResponse(responseCode = "401", description = "Invalid Gitlab Token"),
            @APIResponse(responseCode = "200", description = "Returns the hook given.") })
    @Operation(summary = "Entry point for update notifications")
    public Response fetchConfigData(@HeaderParam(value = "x-gitlab-token") String gitLabToken, Hook hook) {
        
        if(!token.equals(gitLabToken)) {
            LOGGER.error("Invalid token used");
            return Response.status(Status.UNAUTHORIZED).build();
        }
        
        LOGGER.debug("Status updated {}", hook.didFileChange(statusFile));
        
        if(hook.didFileChange(statusFile)) {
            
            LOGGER.debug("Hook for {}", hook.getProject().getPathWithNamespace());
            engagementService.updateStatus(hook.getCustomerName(), hook.getEngagementName());
        }
        return Response.ok(hook).build();
    }
    
}
