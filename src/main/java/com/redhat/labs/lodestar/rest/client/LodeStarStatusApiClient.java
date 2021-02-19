package com.redhat.labs.lodestar.rest.client;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.labs.lodestar.model.status.VersionManifest;

@ApplicationScoped
@RegisterRestClient(configKey = "lodestar.status.api")
public interface LodeStarStatusApiClient {

    @GET
    @Produces("application/json")
    @Path("/api/v1/version/manifest")
    public VersionManifest getVersionManifestV1();

    @GET
    @Path("/api/v1/status")
    @Produces("application/json")
    public Response getComponentStatus();

}