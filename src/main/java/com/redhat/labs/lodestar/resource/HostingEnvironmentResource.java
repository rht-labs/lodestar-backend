package com.redhat.labs.lodestar.resource;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.HostingEnvOpenShfitRollup;
import com.redhat.labs.lodestar.model.HostingEnvironment;
import com.redhat.labs.lodestar.model.filter.PagingOptions;
import com.redhat.labs.lodestar.service.ConfigService;
import com.redhat.labs.lodestar.service.EngagementService;
import com.redhat.labs.lodestar.service.HostingService;
import com.redhat.labs.lodestar.util.JWTUtils;

@RequestScoped
@Path("/hosting/environments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityScheme(securitySchemeName = "jwt", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@Tag(name = "Hosting", description = "Hosting environment apis")
public class HostingEnvironmentResource {

    @Inject
    JsonWebToken jwt;

    @Inject
    JWTUtils jwtUtils;

    @Inject
    EngagementService engagementService;

    @Inject
    ConfigService configService;

    @Inject
    HostingService hostingService;

    @GET
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "hosting environments have been returned.") })
    @Operation(summary = "Returns engagement hosting environments")
    @Timed(name = "hosting-get-all-timer", unit = MetricUnits.MILLISECONDS)
    public Response getHostingEnvironments(@Context UriInfo uriInfo, @BeanParam PagingOptions pagingOptions) {
        return hostingService.getHostingEnvironments(pagingOptions.getPage(), pagingOptions.getPageSize());
    }

    @GET
    @Path("/engagements/{engagementUuid}")
    @SecurityRequirement(name = "jwt", scopes = {})
    @Timed(name = "hosting-env-engagement-timer", unit = MetricUnits.MILLISECONDS)
    public Response getHostingForEngagementUuid(@PathParam(value = "engagementUuid") String engagementUuid) {
        return hostingService.getHostingEnvironments(engagementUuid);
    }

    @PUT
    @Path("/engagements/{engagementUuid}")
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "403", description = "No write access for type"),
            @APIResponse(responseCode = "200", description = "hosting environments have been returned.") })
    @Timed(name = "hosting-update-timer", unit = MetricUnits.MILLISECONDS)
    public Response updateHostingForEnagementUuid(@PathParam(value = "engagementUuid") String engagementUuid,
            List<HostingEnvironment> hostingEnvironments) {
        Engagement engagement = engagementService.getByUuid(engagementUuid);

        boolean writeable = jwtUtils.isAllowedToWriteEngagement(jwt, configService.getPermission(engagement.getType()));

        if (!writeable) {
            return engagementService.getNotWriteableResponse(engagementUuid, engagement.getType());
        }

        String email = jwtUtils.getUserEmailFromToken(jwt);
        String name = jwtUtils.getUsernameFromToken(jwt);

        return hostingService.updateHostingEnvironments(engagementUuid, name, email, hostingEnvironments);
    }

    @GET
    @Path("/openshift/versions")
    @SecurityRequirement(name = "jwt", scopes = {})
    @Timed(name = "hosting-openshift-versions-timer", unit = MetricUnits.MILLISECONDS)
    public Response getOpenShiftVersions(@QueryParam("depth") final HostingEnvOpenShfitRollup rollup, @QueryParam("region") List<String> region) {
        return hostingService.getOcpVersionRollup(rollup, region);
    }

    @HEAD
    @Path("/subdomain/valid/{engagementUuid}/{subdomain}")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "409", description = "Subdomain is taken by another engagement"),
            @APIResponse(responseCode = "200", description = "Subdomain is able to be used by this engagement.") })
    @SecurityRequirement(name = "jwt", scopes = {})
    @Timed(name = "hosting-valid-subdomain-timer", unit = MetricUnits.MILLISECONDS)
    public Response isSubdomainValid(@PathParam("engagementUuid") String engagementUuid, @PathParam("subdomain") String subdomain) {
        return hostingService.isSubdomainValidResponse(engagementUuid, subdomain);
    }

}