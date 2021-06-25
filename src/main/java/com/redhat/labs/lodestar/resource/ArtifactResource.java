package com.redhat.labs.lodestar.resource;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.lodestar.rest.client.LodeStarGDriveApiClient;

@RequestScoped
@Path("/artifact")
@Produces(MediaType.APPLICATION_JSON)
public class ArtifactResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactResource.class);
    
    @Inject
    JsonWebToken jwt;
    
    @Inject
    @RestClient
    LodeStarGDriveApiClient gdriveClient;

    @GET
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Configuration file data has been returned.") })
    @Operation(summary = "Returns configuration file data.")
    public Response fetchConfigData() {
        
        LOGGER.info(jwt.getRawToken());
        
        return gdriveClient.getAllFolders();
    }

}
