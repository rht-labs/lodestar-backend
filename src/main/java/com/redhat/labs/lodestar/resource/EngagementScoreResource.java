package com.redhat.labs.lodestar.resource;

import com.redhat.labs.lodestar.model.filter.ListFilterOptions;
import com.redhat.labs.lodestar.service.EngagementService;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@RequestScoped
@Path("/engagements")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityScheme(securitySchemeName = "jwt", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class EngagementScoreResource {

    @GET
    @Path("/scores")
    @SecurityRequirement(name = "jwt")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "scores have been returned.") })
    @Operation(summary = "Returns engagement scores")
    @Counted(name = "engagement-get-all-scores-counted")
    @Timed(name = "engagement-get-all-scores-timer", unit = MetricUnits.MILLISECONDS)
    public Response getScores(@Context UriInfo uriInfo, @BeanParam ListFilterOptions filterOptions) {

        return Response.status(Response.Status.NOT_IMPLEMENTED).build();

    }

}