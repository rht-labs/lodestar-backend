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

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.redhat.labs.lodestar.model.filter.ListFilterOptions;
import com.redhat.labs.lodestar.service.ArtifactService;

@RequestScoped
@Path("/engagements")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityScheme(securitySchemeName = "jwt", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@Tag(name = "Artifacts", description = "Artifacts for Engagements")
public class ArtifactResource {

    @Inject
    ArtifactService artifactService;

    @GET
    @Path("/artifacts")
    @SecurityRequirement(name = "jwt")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Artifacts have been returned.") })
    @Operation(summary = "Returns artifact list")
    public Response getArtifacts(@Context UriInfo uriInfo,
            @QueryParam(value = "engagementUuid") String engagementUuid, @QueryParam(value = "type") String type,
            @Parameter(description = "Dashboard View. Includes Customer and Engagement Name") @QueryParam("dash") boolean dashboardView,
            @Parameter(description = "v2 only. use search otherwise") @QueryParam("region") List<String> region,
            @BeanParam ListFilterOptions filterOptions) {

        String version = filterOptions.getApiVersion() == null ? "" : filterOptions.getApiVersion();
        if("v1".equals(version) && filterOptions.getSearch().isPresent()) {
            //TODO convert legacy - this overrides type/region - can't have both
            String[] params = filterOptions.getSearch().get().split("&");

            for (String param : params) {
                String[] keyValues = param.split("=");

                if (keyValues[0].equals("artifacts.type")) {
                    type = keyValues[1];
                }

                if (keyValues[0].equals("engagement_region")) {
                    String[] regionsArray = keyValues[1].split(",");
                    region = Arrays.asList(regionsArray);
                }
            }
        }

        return artifactService.getArtifacts(filterOptions, engagementUuid, type, region);
     }

    @GET
    @Path("/artifact/types")
    @SecurityRequirement(name = "jwt")
    public Set<String> legacyPath(@QueryParam("regions") List<String> regions) {
        return getArtifactTypes(regions);
    }

    @GET
    @Path("/artifacts/types")
    @SecurityRequirement(name = "jwt")
    public Set<String> getArtifactTypes(@QueryParam("regions") List<String> regions) {
        return artifactService.getTypes(regions);
    }

    @GET
    @Path("/artifacts/types/counts")
    @SecurityRequirement(name = "jwt")
    public Response getArtifactTypesCount(@QueryParam("regions") List<String> regions) {
        return artifactService.getTypesCount(regions);
    }
}