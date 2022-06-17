package com.redhat.labs.lodestar.rest.client;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.labs.lodestar.exception.mapper.ServiceResponseMapper;

@ApplicationScoped
@RegisterRestClient(configKey = "lodestar.config.api")
@RegisterProvider(value = ServiceResponseMapper.class, priority = 50)
@Path("/api/v1/configs/runtime")
@Produces("application/json")
public interface ConfigApiClient {

    @GET
    Response getRuntimeConfig(@QueryParam("type") String type);

    @GET
    @Path("/rbac")
    Map<String, List<String>> getPermission();

    @GET
    @Path("artifact/options")
    Map<String, String> getArtifactOptions();

    @GET
    @Path("participant/options")
    Map<String, String> getParticipantOptions(@QueryParam("engagementType") String type);

    @GET
    @Path("participant/options")
    Map<String, String> getParticipantOptions();

    @GET
    @Path("engagement/options")
    Map<String, String> getEngagementOptions();

    @GET
    @Path("region/options")
    Map<String, String> getRegionOptions();

}
