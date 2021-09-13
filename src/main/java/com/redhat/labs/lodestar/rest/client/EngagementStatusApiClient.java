package com.redhat.labs.lodestar.rest.client;

import com.redhat.labs.lodestar.exception.mapper.ServiceResponseMapper;
import com.redhat.labs.lodestar.model.Status;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@ApplicationScoped
@RegisterRestClient(configKey = "lodestar.engagement.status.api")
@RegisterProvider(value = ServiceResponseMapper.class, priority = 50)
@Produces("application/json")
@Consumes("application/json")
@ClientHeaderParam(name = "version", value = "v1")
@Path("/api/engagement/status")
public interface EngagementStatusApiClient {

    @GET
    @Path("{engagementUuid}")
    Status getEngagementStatus(@PathParam("engagementUuid") String engagementUuid);

    @PUT
    @Path("{engagementUuid}")
    Response updateEngagementStatus(@PathParam("engagementUuid") String engagementUuid);

    @PUT
    @Path("refresh")
    Response refresh();
}
