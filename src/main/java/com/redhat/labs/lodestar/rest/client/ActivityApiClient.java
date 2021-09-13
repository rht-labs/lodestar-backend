package com.redhat.labs.lodestar.rest.client;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import com.redhat.labs.lodestar.model.*;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import com.redhat.labs.lodestar.exception.mapper.ServiceResponseMapper;

import java.util.*;

@ApplicationScoped
@RegisterRestClient(configKey = "lodestar.activity.api")
@RegisterProvider(value = ServiceResponseMapper.class, priority = 50)
@Produces("application/json")
@Consumes("application/json")
@ClientHeaderParam(name = "version", value = "v1")
@Path("api/activity")
public interface ActivityApiClient {

    @POST
    @Path("/hook")
    Response postHook(Hook hook, @HeaderParam("x-gitlab-token") String hookToken);
    
    @GET
    @Path("/uuid/{engagementUuid}")
    List<Commit> getActivityForUuid(@PathParam("engagementUuid") String engagementUuid);

    @HEAD
    @Path("{engagementUuid}")
    Response getLastActivity(@PathParam("engagementUuid") String engagementUuid);

    @GET
    @Path("/latest")
    List<Commit> getLatestActivity(@QueryParam("page") int page, @QueryParam("pageSize") int pageSize);
    
    @GET
    @Path("/uuid/{engagementUuid}")
    Response getPaginatedActivityForUuid(@PathParam("engagementUuid") String engagementUuid, @QueryParam("page") int page, @QueryParam("pageSize") int pageSize);
    
    @GET
    Response getPaginatedActivity(@QueryParam("page") int page, @QueryParam("pageSize") int pageSize);
    
    @PUT
    @Path("/refresh")
    Response refresh();

}
