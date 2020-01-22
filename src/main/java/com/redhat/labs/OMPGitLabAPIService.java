package com.redhat.labs;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;


@RegisterRestClient(configKey = "omp.gitlab.api")
public interface OMPGitLabAPIService {
    @GET
    @Path("/api/file")
    @Produces("application/json")
    Response getFile(@QueryParam("name") String name, @QueryParam("repo_id") String repoId);

    @POST
    @Path("/api/residencies")
    @Produces("application/json")
    Response createNewResidency(Object residency);
}
