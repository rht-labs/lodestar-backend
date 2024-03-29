package com.redhat.labs.lodestar.rest.client;

import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.BeanParam;
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
import com.redhat.labs.lodestar.model.Artifact;
import com.redhat.labs.lodestar.model.filter.ArtifactOptions;

@ApplicationScoped
@RegisterRestClient(configKey = "lodestar.artifacts.api")
@RegisterProvider(value = ServiceResponseMapper.class, priority = 50)
@Produces("application/json")
@Consumes("application/json")
@ClientHeaderParam(name = "version", value = "v1")
@Path("/api/artifacts")
public interface ArtifactApiClient {

    @PUT
    @Path("/engagement/uuid/{engagementUuid}/{region}")
    Response updateArtifacts(@PathParam(value = "engagementUuid") String engagementUuid, @PathParam(value = "region") String region,
                             List<Artifact> artifacts, @QueryParam("authorEmail") String authorEmail, @QueryParam("authorName") String authorName);

    @GET
    Response getArtifacts(@BeanParam ArtifactOptions options);

    @GET
    @Path("/count")
    Response getArtifactCount(@BeanParam ArtifactOptions options);

    @GET
    @Path("/types")
    Set<String> getTypes(@QueryParam("regions") List<String> regions);

    @GET
    @Path("types/count")
    Response getTypesCount(@QueryParam("regions") List<String> regions);

    @PUT
    @Path("/refresh")
    Response refreshArtifacts();
}
