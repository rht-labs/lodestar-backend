package com.redhat.labs.omp.resources;

import javax.annotation.security.PermitAll;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.omp.cache.ResidencyDataCache;
import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.omp.service.OMPGitLabAPIService;

@Path("/engagements")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class EngagementResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(EngagementResource.class);
 
    @Inject
    JsonWebToken jwt;

    @Inject
    @RestClient
    OMPGitLabAPIService gitApi;

    @Inject
    ResidencyDataCache engagementCache;

    @APIResponses( value = {
    		@APIResponse( responseCode = "201", 
    				description = "Engagement created. Location in header",
    				content = @Content(mediaType = "application/json")
    				)
    })
    @Operation(summary = "Create an engagement persisted to Gitlab",
    		description = "Engagement creates a new project in Gitlab")
    @POST
    @PermitAll
    public Response createEngagement(Engagement engagement, @Context UriInfo uriInfo) {

        //TODO add security here - ie replaces Permits all
        LOGGER.debug(engagement.toString());

        Response gitResponse = gitApi.createEngagement(engagement);

        if(gitResponse.getStatus() == 201) {
            String location = gitResponse.getHeaderString("Location");
            String projectId = location.substring(location.lastIndexOf("/"));

            UriBuilder builder = uriInfo.getAbsolutePathBuilder();
            builder.path(projectId);
            return Response.created(builder.build()).build();
        }

        return gitResponse;

    }
}