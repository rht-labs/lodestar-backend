package com.redhat.labs.omp.rest.client;

import javax.ws.rs.DELETE;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.labs.omp.model.git.api.GitApiEngagement;
import com.redhat.labs.omp.model.git.api.GitApiFile;

@RegisterRestClient(configKey = "omp.gitlab.api")
public interface OMPGitLabAPIService {

    @POST
    @Path("/api/v1/engagements")
    @Produces("application/json")
    Response createEngagement(GitApiEngagement engagement, @QueryParam("username") String username,
            @QueryParam("userEmail") String userEmail);

    @POST
    @Path("/api/v1/projects/{projectId}/files")
    @Produces("application/json")
    Response createFile(@PathParam("projectId") Integer projectId, GitApiFile file);

    @PUT
    @Path("/api/v1/projects/{projectId}/files")
    @Produces("application/json")
    Response updateFile(@PathParam("projectId") Integer projectId, GitApiFile file);

    @GET
    @Path("/api/v1/projects/{projectId}/files/{filePath}")
    @Produces("application/json")
    GitApiFile getFile(@PathParam("projectId") Integer projectId, @PathParam("filePath") @Encoded String filePath);

    @DELETE
    @Path("/api/v1/projects/{projectId}/files/{filePath}")
    Response deleteFile(@PathParam("projectId") Integer projectId, @PathParam("filePath") @Encoded String filePath,
            @QueryParam("username") String username, @QueryParam("userEmail") String userEmail);

}
