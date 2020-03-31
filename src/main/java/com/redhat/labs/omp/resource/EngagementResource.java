package com.redhat.labs.omp.resource;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.omp.service.EngagementService;

@Path("/engagements")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class EngagementResource {

	@Inject
	JsonWebToken jwt;

	@Inject
	EngagementService engagementService;

	@POST
	@APIResponses(value = {
			@APIResponse(responseCode = "201", description = "Engagement created. Location in header", content = @Content(mediaType = "application/json")) })
	@Operation(summary = "Create an engagement persisted to Gitlab", description = "Engagement creates a new project in Gitlab")
	public Response createEngagement(Engagement engagement, @Context UriInfo uriInfo) {
		return engagementService.createEngagement(engagement, uriInfo);
	}

}