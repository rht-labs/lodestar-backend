package com.redhat.labs.lodestar.rest.client;

import com.redhat.labs.lodestar.exception.mapper.ServiceResponseMapper;
import com.redhat.labs.lodestar.model.Engagement;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.*;

@ApplicationScoped
@RegisterRestClient(configKey = "lodestar.engagements.api")
@RegisterProvider(value = ServiceResponseMapper.class, priority = 50)
@Path("/api/v2/engagements")
public interface EngagementApiClient {

    @GET
    Response getEngagements(@QueryParam("page") int page, @QueryParam("pageSize") int pageSize, @QueryParam("region") Set<String> region,
                            @QueryParam("types") Set<String> types, @QueryParam("inStates") Set<String> states, @QueryParam("q") String search,
                            @QueryParam("category") String category, @QueryParam("sort") String sort);

    //TODO support time shifting
    @GET
    @Path("count")
    Map<Engagement.EngagementState, Integer> getEngagementCounts(@QueryParam("region") Set<String> region);

    @GET
    @Path("category/{category}")
    List<Engagement> getEngagementsWithCategory(@QueryParam("page") int page, @QueryParam("pageSize") int pageSize, @QueryParam("region") Set<String> region,
                            @QueryParam("types") Set<String> types, @QueryParam("inStates") Set<String> states, @PathParam("category") String category, @QueryParam("sort") String sort);

    @GET
    @Path("{uuid}")
    Engagement getEngagement(@PathParam("uuid") String uuid);

    @HEAD
    @Path("{uuid}")
    Response getEngagementHead(@PathParam("uuid") String uuid);

    @GET
    @Path("project/{id}")
    Engagement getEngagementByProject(@PathParam("id") int projectId);

    @GET
    @Path("customer/{customer}/engagement/{engagement}")
    Engagement getEngagement(@PathParam("customer") String customer, @PathParam("engagement") String engagementName);

    @DELETE
    @Path("{uuid}")
    Response deleteEngagement(@PathParam("uuid") String uuid);

    @POST
    Engagement createEngagement(Engagement engagement);

    @PUT
    Response updateEngagement(Engagement engagement);

    @PUT
    @Path("{uuid}/lastUpdate")
    Response registerUpdate(@PathParam("uuid") String uuid);

    @PUT
    @Path("{uuid}/launch")
    Response launch(@PathParam("uuid") String uuid, @QueryParam("author") String author, @QueryParam("authorEmail") String authorEmail);

    @PUT
    @Path("refresh")
    Response refresh(@QueryParam("uuids") Set<String> uuids);

    @GET
    @Path("suggest")
    Response suggest(@QueryParam("partial") String partial);

}
