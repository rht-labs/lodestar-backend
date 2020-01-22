package com.redhat.labs;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;


@RegisterRestClient(configKey = "omp.gitlab.api")
public interface OMPGitLabAPIService {
    @GET
    @Path("/api/file")
    @Produces("application/json")
    Response getFile(@QueryParam("name") String name, @QueryParam("repo_id") String repoId);
}
