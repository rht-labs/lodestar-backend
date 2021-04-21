package com.redhat.labs.lodestar.resource;

import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

import com.redhat.labs.lodestar.model.filter.ListFilterOptions;
import com.redhat.labs.lodestar.model.pagination.PagedArtifactResults;
import com.redhat.labs.lodestar.model.pagination.PagedStringResults;
import com.redhat.labs.lodestar.service.EngagementService;

@RequestScoped
@Path("/engagements")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityScheme(securitySchemeName = "jwt", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class EngagementArtifactResource {

    @Inject
    JsonWebToken jwt;

    @Inject
    EngagementService engagementService;

    @GET
    @Path("/artifacts")
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Artifacts have been returned.") })
    @Operation(summary = "Returns artifact list")
    @Counted(name = "engagement-get-all-artifacts-counted")
    @Timed(name = "engagement-get-all-artifacts-timer", unit = MetricUnits.MILLISECONDS)
    public Response getArtifacts(@Context UriInfo uriInfo,
            @Parameter(name = "suggest", deprecated = true, required = false, description = "uses suggestion as case insensitive search string") @QueryParam("suggest") Optional<String> suggest,
            @BeanParam ListFilterOptions filterOptions) {

        if (suggest.isPresent()) {
            filterOptions.addLikeSearchCriteria("artifacts.type", suggest.get());
        }

        PagedArtifactResults page = engagementService.getArtifacts(filterOptions);
        ResponseBuilder builder = Response.ok(page.getResults()).links(page.getLinks(uriInfo.getAbsolutePathBuilder()));
        page.getHeaders().entrySet().stream().forEach(e -> builder.header(e.getKey(), e.getValue()));
        return builder.build();

    }

    @GET
    @Path("/artifact/types")
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Artifact types have been returned.") })
    @Operation(summary = "Returns artifact type list")
    @Counted(name = "engagement-get-all-artifacts-types-counted")
    @Timed(name = "engagement-get-all-artifacts-types-timer", unit = MetricUnits.MILLISECONDS)
    public Response getArtifactTypes(@Context UriInfo uriInfo,
            @Parameter(name = "suggest", deprecated = true, required = false, description = "uses suggestion as case insensitive search string") @QueryParam("suggest") Optional<String> suggest,
            @BeanParam ListFilterOptions filterOptions) {

        if (suggest.isPresent()) {
            filterOptions.addLikeSearchCriteria("artifacts.type", suggest.get());
        }

        PagedStringResults page = engagementService.getArtifactTypes(filterOptions);
        ResponseBuilder builder = Response.ok(page.getResults()).links(page.getLinks(uriInfo.getAbsolutePathBuilder()));
        page.getHeaders().entrySet().stream().forEach(e -> builder.header(e.getKey(), e.getValue()));
        return builder.build();

    }

}