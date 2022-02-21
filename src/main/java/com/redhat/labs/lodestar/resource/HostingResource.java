package com.redhat.labs.lodestar.resource;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import com.redhat.labs.lodestar.model.HostingEnvironment;
import com.redhat.labs.lodestar.model.filter.PagingFilter;
import com.redhat.labs.lodestar.service.HostingService;
import com.redhat.labs.lodestar.util.JWTUtils;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

import com.redhat.labs.lodestar.model.filter.ListFilterOptions;
import com.redhat.labs.lodestar.model.pagination.PagedHostingEnvironmentResults;
import com.redhat.labs.lodestar.service.EngagementService;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.*;

@RequestScoped
@Path("/engagements/hosting/environments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityScheme(securitySchemeName = "jwt", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@Tag(name = "Hosting Environments", description = "Hosting Environment Info")
public class HostingResource {

    @Inject
    HostingService hostingService;

    @Inject
    JsonWebToken jwt;

    @Inject
    JWTUtils jwtUtils;

    @GET
    @SecurityRequirement(name = "jwt")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "hosting environments have been returned.") })
    @Operation(summary = "Returns engagement hosting environments")
    public Response getHostingEnvironments(@QueryParam("engagementUuids") Set<String> engagementUuids, @BeanParam PagingFilter pagingFilter) {

        return hostingService.getHostingEnvironments(engagementUuids, pagingFilter.getPage(), pagingFilter.getPageSize());
    }

    @GET
    @Path("engagement/{engagementUuid}")
    @SecurityRequirement(name = "jwt")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "hosting environments have been returned.") })
    @Operation(summary = "Returns engagement hosting environments")
    public Response getHostingEnvironment(@PathParam("engagementUuid") String engagementUuid) {
        return Response.ok(hostingService.getHostingEnvironments(engagementUuid)).build();
    }

    @PUT
    @Path("engagement/{engagementUuid}")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "hosting environments updated.") })
    @Operation(summary = "Returns updated engagement hosting environments")
    public Response updateHostingEnvForEngagement(@PathParam("engagementUuid") String engagementUuid, List<HostingEnvironment> hostingEnvironments) {
        return Response.ok(hostingService.updateAndReload(engagementUuid, hostingEnvironments, jwtUtils.getAuthorFromToken(jwt))).build();
    }

}