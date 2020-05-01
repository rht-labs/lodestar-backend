package com.redhat.labs.omp.rest.client;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.omp.model.Version;

@RegisterRestClient(configKey = "omp.gitlab.api")
public interface OMPGitLabAPIService {

    @GET
    @Path("/api/v1/engagements")
    @Produces("application/json")
    List<Engagement> getEngagments();

    @POST
    @Path("/api/v1/engagements")
    @Produces("application/json")
    Response createOrUpdateEngagement(Engagement engagement, @QueryParam("username") String username,
            @QueryParam("userEmail") String userEmail);

    @GET
    @Path("/api/v1/config")
    @Produces("application/json")
    Response getConfigFile();

    @GET
    @Path("/api/v2/config")
    @Produces("application/json")
    Response getConfigFileV2();

    @GET
    @Path("/api/v1/version")
    @Produces("application/json")
    Version getVersion();

}
