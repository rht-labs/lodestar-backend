package com.redhat.labs.lodestar.resource;

import com.redhat.labs.lodestar.model.filter.ListFilterOptions;
import com.redhat.labs.lodestar.service.CategoryService;
import com.redhat.labs.lodestar.service.EngagementService;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@RequestScoped
@Path("/engagements/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityScheme(securitySchemeName = "jwt", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class EngagementCategoryResource {

    @Inject
    EngagementService engagementService;

    @Inject
    CategoryService categoryService;
    
    @GET
    @SecurityRequirement(name = "jwt")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Customer data has been returned.") })
    @Operation(summary = "Returns customers list")
    public Response getAllCategories(@Context UriInfo uriInfo, @BeanParam ListFilterOptions filterOptions) {

        List<String> regions = new ArrayList<>();
        if(filterOptions.getSearch().isPresent()) {
            //TODO convert legacy -
            String[] params = filterOptions.getSearch().get().split("&");

            for (String param : params) {
                String[] keyValues = param.split("=");

                if (keyValues[0].equals("engagement_region")) {
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
    @SecurityRequirement(name = "jwt")
    public Set<String> getSuggestions(@QueryParam("q") String partial) {
        return categoryService.getCategorySuggestions(partial);
    }

}