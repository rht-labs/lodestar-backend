package com.redhat.labs.lodestar.rest.client;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.labs.lodestar.exception.mapper.ServiceResponseMapper;
import com.redhat.labs.lodestar.model.EngagementUser;

@ApplicationScoped
@RegisterRestClient(configKey = "lodestar.participants.api")
@RegisterProvider(value = ServiceResponseMapper.class, priority = 50)
@Produces("application/json")
@Consumes("application/json")
@ClientHeaderParam(name = "version", value = "v1")
@Path("/api/participants")
public interface ParticipantApiClient {

    @GET
    Response getParticipants(@QueryParam("engagementUuids") Set<String> engagementUuids,
            @QueryParam("page") Integer page, @QueryParam("pageSize") Integer pageSize);

    @GET
    @Path("/engagements/uuid/{engagementUuid}")
    List<EngagementUser> getParticipantsForEngagement(@PathParam("engagementUuid") String uuid);

    @PUT
    @Path("/engagements/uuid/{engagementUuid}/{region}")
    Response updateParticipants(@PathParam(value = "engagementUuid") String engagementUuid, @PathParam(value = "region") String region,
            @QueryParam(value = "authorName") String authorName, @QueryParam(value = "authorEmail") String authorEmail,
            Set<EngagementUser> participants);

    @GET
    @Path("/enabled")
    Map<String, Long> getEnabledParticipants(@QueryParam("region") List<String> region);
    
    @GET
    @Path("/enabled/breakdown")
    Map<String,Map<String, Long>> getEnabledParticipantsAllRegions();

    @PUT
    @Path("/refresh")
    Response refreshParticipants();
}
