package com.redhat.labs.omp.rest.client;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.labs.omp.model.status.VersionManifestV1;

@ApplicationScoped
@RegisterRestClient(configKey = "lodestar.status.api")
public interface LodeStarStatusApiClient {

    @GET
    @Produces("application/json")
    @Path("/api/v1/version/manifest")
    public VersionManifestV1 getVersionManifestV1();

    @GET
    @Produces("application/json")
    @Path("/api/v1/status")
    @APIResponses(value = { 
            @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Configuration file data has been returned.") })
    @Operation(summary = "Returns configuration file data from git.")
    public Response getComponentStatus();

}