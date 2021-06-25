package com.redhat.labs.lodestar.rest.client;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import com.redhat.labs.lodestar.exception.mapper.LodeStarGitLabAPIServiceResponseMapper;
import com.redhat.labs.lodestar.model.Hook;

@ApplicationScoped
@RegisterRestClient(configKey = "lodestar.activity.api")
@RegisterProvider(value = LodeStarGitLabAPIServiceResponseMapper.class, priority = 50)
@Produces("application/json")
@Consumes("application/json")
@ClientHeaderParam(name = "version", value = "v1")
public interface LodeStarActivityApiClient {

    @POST
    @Path("/api/activity/hook")
    public Response postHook(Hook hook, @HeaderParam("x-gitlab-token") String hookToken);
    
    @GET
    @Path("/api/activity/uuid/{engagementUuid}")
    public Response getActivityForUuid(@PathParam("engagementUuid") String engagementUuid);
    
    @GET
    @Path("/api/activity/uuid/{engagementUuid}")
    Response getPaginatedActivityForUuid(@PathParam("engagementUuid") String engagementUuid, @QueryParam("page") int page, @QueryParam("pageSize") int pageSize);
    
    @GET
    @Path("/api/activity")
    Response getPaginatedActivity(@QueryParam("page") int page, @QueryParam("pageSize") int pageSize);
    
    @PUT
    @Path("/api/activity/refresh")
    public Response refresh();

}
