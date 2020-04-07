package com.redhat.labs.omp.rest.client;

import java.util.concurrent.CompletionStage;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.omp.model.File;

@RegisterRestClient(configKey = "omp.gitlab.api")
public interface OMPGitLabAPIService {

    @GET
    @Path("/api/file")
    @Produces("application/json")
    Response getFile(@QueryParam("name") String name, @QueryParam("repo_id") String repoId);

    @POST
    @Path("/api/v1/engagements")
    @Produces("application/json")
    CompletionStage<Response> createEngagement(Engagement engagement);

    @POST
    @Path("/api/v1/projects/{projectId}/files")
    @Produces("application/json")
    Response createFile(@PathParam("projectId") Integer projectId);

    @PUT
    @Path("/api/v1/projects/{projectId}/files")
    @Produces("application/json")
    Response updateFile(@PathParam("projectId") Integer projectId, File file);

    @GET
    @Path("/api/v1/projects/{projectId}/files/{filePath}")
    @Produces("application/json")
    Response createFile(@PathParam("projectId") Integer projectId, @PathParam("filePath") String filePath);

}
