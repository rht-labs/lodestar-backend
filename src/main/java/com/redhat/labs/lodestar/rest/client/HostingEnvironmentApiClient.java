package com.redhat.labs.lodestar.rest.client;

import com.redhat.labs.lodestar.exception.mapper.ServiceResponseMapper;
import com.redhat.labs.lodestar.model.HostingEnvironment;
import com.redhat.labs.lodestar.model.HostingRollup;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Set;

@ApplicationScoped
@RegisterRestClient(configKey = "lodestar.hosting.api")
@RegisterProvider(value = ServiceResponseMapper.class, priority = 50)
@Produces("application/json")
@Consumes("application/json")
@ClientHeaderParam(name = "version", value = "v1")
@Path("/api/hosting")
public interface HostingEnvironmentApiClient {

    @GET
    Response getHostingEnvironments(@QueryParam("engagementUuids") Set<String> engagementUuids, @QueryParam(value = "page") int page,
                                    @QueryParam(value = "pageSize") int pageSize);

    @GET
    @Path("/engagements/{engagementUuid}")
    List<HostingEnvironment> getHostingEnvironmentsByEngagementUuid(@PathParam("engagementUuid") String engagementUuid);

    @PUT
    @Path("/engagements/{engagementUuid}")
    List<HostingEnvironment> updateHostingEnvironments(@PathParam("engagementUuid") String uuid, List<HostingEnvironment> hostingEnvironments,
                                              @QueryParam(value = "authorEmail") String authorEmail,
                                              @QueryParam(value = "authorName") String authorName);

    @HEAD
    @Path("/subdomain/valid/{engagementUuid}/{subdomain}")
    Response isSubdomainValid(@PathParam("engagementUuid") String engagementUuid, @PathParam("subdomain") String subdomain);

    @GET
    @Path("/openshift/versions")
    Response getOpenShiftVersions(@QueryParam("depth") final HostingRollup rollup, @QueryParam("region") List<String> region);

    @PUT
    @Path("/refresh")
    Response refresh();
}
