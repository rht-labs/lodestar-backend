package com.redhat.labs.lodestar.resource;

import java.util.*;

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
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.redhat.labs.lodestar.service.CategoryService;
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
import com.redhat.labs.lodestar.model.pagination.PagedCategoryResults;
import com.redhat.labs.lodestar.service.EngagementService;

@RequestScoped
@Path("/engagements/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityScheme(securitySchemeName = "jwt", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class EngagementCategoryResource {

    @Inject
    JsonWebToken jwt;

    @Inject
    EngagementService engagementService;

    @Inject
    CategoryService categoryService;
    
    @GET
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Customer data has been returned.") })
    @Operation(summary = "Returns customers list")
    public Response getAllCategories(@Context UriInfo uriInfo, @BeanParam ListFilterOptions filterOptions) {

        List<String> regions = new ArrayList<>();
        if(filterOptions.getSearch().isPresent()) {
            //TODO convert legacy -
            String params[] = filterOptions.getSearch().get().split("&");

            for(int i=0; i< params.length; i++) {
                String[] keyValues = params[i].split("=");

                if(keyValues[0].equals("engagement_region")) {
                    String[] regionsArray = keyValues[1].split(",");
                    regions = Arrays.asList(regionsArray);
                }
            }
        }

        return engagementService.getCategories(regions, filterOptions);
    }

    //TODO page or limit?
    @GET
    @Path("suggest")
    @SecurityRequirement(name = "jwt", scopes = {})
    public Set<String> getSuggestions(@QueryParam("q") String partial) {
        return categoryService.getCategorySuggestions(partial);
    }

}