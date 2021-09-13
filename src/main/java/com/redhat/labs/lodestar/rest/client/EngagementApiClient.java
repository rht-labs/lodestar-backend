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
    List<Engagement> getEngagements(@QueryParam("page") int page, @QueryParam("pageSize") int pageSize);

    //TODO support time shifting
    @GET
    @Path("count")
    Map<Engagement.EngagementState, Integer> getEngagementCounts();

    @GET
    @Path("category/{category}")
    List<Engagement> getEngagementsWithCategory(@PathParam("category") String category);

    @GET
    @Path("{uuid}")
    Engagement getEngagement(@PathParam("uuid") String uuid);

    @HEAD
    @Path("{uuid}")
    Response getEngagementHead(@PathParam("uuid") String uuid);

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
    @Path("{uuid}/launch")
    Response launch(@PathParam("uuid") String uuid, @QueryParam("author") String author, @QueryParam("authorEmail") String authorEmail);

    @PUT Response refresh();

    @GET
    @Path("suggest")
    Response suggest(@QueryParam("partial") String partial);

}
