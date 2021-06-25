package com.redhat.labs.lodestar.rest.client;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

//@ApplicationScoped
//@RegisterRestClient(configKey = "lodestar.gdrive.api")
//@RegisterClientHeaders(JWTRequestFactory.class)
public interface LodeStarGDriveApiClient {

    @GET
    @Path("/folders")
    @Produces("application/json")
    public Response getAllFolders();
}
