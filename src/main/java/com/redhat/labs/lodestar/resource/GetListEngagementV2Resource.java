package com.redhat.labs.lodestar.resource;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
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
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

import com.redhat.labs.lodestar.model.filter.ListFilterOptions;
import com.redhat.labs.lodestar.model.filter.SimpleFilterOptions;
import com.redhat.labs.lodestar.model.pagination.PagedCategoryResults;
import com.redhat.labs.lodestar.model.pagination.PagedEngagementResults;
import com.redhat.labs.lodestar.model.pagination.PagedStringResults;
import com.redhat.labs.lodestar.service.EngagementService;

@RequestScoped
@Path("/api/v2/engagements")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityScheme(securitySchemeName = "jwt", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class GetListEngagementV2Resource {

    @Inject
    JsonWebToken jwt;

    @Inject
    EngagementService engagementService;

    @GET
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "A list or empty list of engagement resources returned") })
    @Operation(summary = "Returns all engagement resources from the database.  Can be empty list if none found.")
    @Counted(name = "engagement-get-all-counted")
    @Timed(name = "engagement-get-all-timer", unit = MetricUnits.MILLISECONDS)
    public Response getAll(@Context UriInfo uriInfo, @BeanParam ListFilterOptions filterOptions) {

        // set defaults for paging if not already set
        setPagingDefaults(filterOptions);

        PagedEngagementResults page = engagementService.getEngagementsPaged(filterOptions);
        ResponseBuilder builder = Response.ok(page.getResults()).links(page.getLinks(uriInfo.getAbsolutePathBuilder()));
        page.getHeaders().entrySet().stream().forEach(e -> builder.header(e.getKey(), e.getValue()));
        return builder.build();

    }

    @GET
    @Path("/customers")
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Customer data has been returned.") })
    @Operation(summary = "Returns customers list")
    @Counted(name = "engagement-suggest-url-counted")
    @Timed(name = "engagement-suggest-url-timer", unit = MetricUnits.MILLISECONDS)
    public Response findCustomers(@Context UriInfo uriInfo, @BeanParam SimpleFilterOptions filterOptions) {

        PagedStringResults page = engagementService.getSuggestions(filterOptions);
        ResponseBuilder builder = Response.ok(page.getResults()).links(page.getLinks(uriInfo.getAbsolutePathBuilder()));
        page.getHeaders().entrySet().stream().forEach(e -> builder.header(e.getKey(), e.getValue()));
        return builder.build();

    }

    @GET
    @Path("/categories")
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Customer data has been returned.") })
    @Operation(summary = "Returns customers list")
    @Counted(name = "engagement-get-all-categories-counted")
    @Timed(name = "engagement-get-all-categories-timer", unit = MetricUnits.MILLISECONDS)
    public Response getAllCategories(@Context UriInfo uriInfo, @BeanParam SimpleFilterOptions filterOptions) {

        PagedCategoryResults page = engagementService.getCategories(filterOptions);
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
    @Counted(name = "engagement-get-all-artifacts-counted")
    @Timed(name = "engagement-get-all-artifacts-timer", unit = MetricUnits.MILLISECONDS)
    public Response getArtifactTypes(@Context UriInfo uriInfo, @BeanParam SimpleFilterOptions filterOptions) {
        
        PagedStringResults page = engagementService.getArtifactTypes(filterOptions);
        ResponseBuilder builder = Response.ok(page.getResults()).links(page.getLinks(uriInfo.getAbsolutePathBuilder()));
        page.getHeaders().entrySet().stream().forEach(e -> builder.header(e.getKey(), e.getValue()));
        return builder.build();
    }

    /**
     * Sets the default values for Paging Results. If not set, page number is set to
     * 1 and results per page is set to 20.
     * 
     * @param filterOptions
     */
    private void setPagingDefaults(ListFilterOptions filterOptions) {

        if (filterOptions.getPage().isEmpty()) {
            filterOptions.setPage(1);
        }

        if (filterOptions.getPerPage().isEmpty()) {
            filterOptions.setPerPage(20);
        }

    }

}
