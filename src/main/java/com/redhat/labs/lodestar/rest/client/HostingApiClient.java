package com.redhat.labs.lodestar.rest.client;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.labs.lodestar.exception.mapper.LodeStarGitLabAPIServiceResponseMapper;
import com.redhat.labs.lodestar.model.HostingEnvOpenShfitRollup;
import com.redhat.labs.lodestar.model.HostingEnvironment;

@ApplicationScoped
@RegisterRestClient(configKey = "lodestar.hosting.api")
@RegisterProvider(value = LodeStarGitLabAPIServiceResponseMapper.class, priority = 50)
@Produces("application/json")
@Consumes("application/json")
@ClientHeaderParam(name = "version", value = "v1")
@Path("/api/hosting")
public interface HostingApiClient {

    @GET
    Response getHostingEnvironments(@QueryParam("page") int page, @QueryParam("pageSize") int pageSize);

    @GET
    @Path("/engagements/{engagementUuid}")
    Response getHostingEnvironmentsForEngagement(@PathParam("engagementUuid") String engagementUuid);

    @PUT
    @Path("/engagements/{engagementUuid}")
    Response updateHostingEnvironments(@PathParam(value = "engagementUuid") String engagementUuid,
            @QueryParam(value = "authorEmail") String authorEmail, @QueryParam(value = "authorName") String authorName,
            List<HostingEnvironment> hostingEnvironments);

    @Path("/openshift/versions")
    @GET
    public Response getOpenShiftVersions(@QueryParam("depth") final HostingEnvOpenShfitRollup rollup, @QueryParam("region") List<String> region);

    @PUT
    @Path("/refresh")
    Response refreshHostingEnvironments();

    @HEAD
    @Path("/subdomain/valid/{engagementUuid}/{subdomain}")
    Response isSubdomainValid(@PathParam("engagementUuid") String engagementUuid, @PathParam("subdomain") String subdomain);
}
