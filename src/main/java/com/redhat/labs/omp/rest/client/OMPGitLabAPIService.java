package com.redhat.labs.omp.rest.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.labs.omp.model.Engagement;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;


@RegisterRestClient(configKey = "omp.gitlab.api")
public interface OMPGitLabAPIService {

    @GET
    @Path("/api/file")
    @Produces("application/json")
    Response getFile(@QueryParam("name") String name, @QueryParam("repo_id") String repoId);

    @POST
    @Path("/api/v1/engagements")
    @Produces("application/json")
    Response createEngagement(Engagement engagement);

}
