package com.redhat.labs.lodestar.resource;

import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.lodestar.service.ConfigService;

@RequestScoped
@Path("/config")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Config", description = "Resource to pull configuration info")
public class ConfigResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigResource.class);

    @Inject
    ConfigService configService;

    @GET
    @SecurityRequirement(name = "jwt")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Configuration file data has been returned.") })
    @Operation(summary = "Returns configuration file data.")
    public Response fetchConfigData(@QueryParam("type") Optional<String> type) {
        LOGGER.debug("Requested runtime configuration type {}", type);
        return configService.getRuntimeConfig(type);
    }
    
    @PUT
    @Path("/rbac/cache")
    @SecurityRequirement(name = "jwt")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Cache invalidated.") })
    public Response invalidateRbacCache() {
        configService.invalidateCache();
        return Response.ok().build();
    }

    @GET
    @Path("artifact/options")
    @SecurityRequirement(name = "jwt")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Artifact options success.") })
    @Operation(summary = "Returns a map of key value pairs of artifact options.")
    public Map<String, String> getArtifactOptions() {
        return configService.getArtifactOptions();
    }

    @GET
    @Path("participant/options")
    @SecurityRequirement(name = "jwt")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Participant options success.") })
    @Operation(summary = "Returns a map of key value pairs of participant options. " +
            "If the engagement type is not found it will return the default values (Residency)")
    public Map<String, String> getParticipantOptions(@QueryParam("engagementType") String type) {
        if(type == null) {
            return configService.getParticipantOptions();
        }

        return configService.getParticipantOptions(type);
    }

    @GET
    @Path("engagement/options")
    @SecurityRequirement(name = "jwt")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Engagement options success.") })
    @Operation(summary = "Returns a map of key value pairs or engagement option.")
    public Map<String, String> getEngagementOptions() {
        return configService.getEngagementOptions();
    }

    @GET
    @Path("region/options")
    @SecurityRequirement(name = "jwt")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Region options success.") })
    @Operation(summary = "Returns a map of key value pairs or region option.")
    public Map<String, String> getRegionOptions() {
        return configService.getRegionOptions();
    }
}
