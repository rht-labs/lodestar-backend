package com.redhat.labs.lodestar.resource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.redhat.labs.lodestar.model.EngagementUser;
import com.redhat.labs.lodestar.service.ParticipantService;
import com.redhat.labs.lodestar.util.JWTUtils;

@RequestScoped
@Path("/engagements/participants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityScheme(securitySchemeName = "jwt", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@Tag(name = "Participants", description = "Participants in Engagements")
public class ParticipantResource {
    private static final String DEFAULT_PAGE_SIZE = "100";
    private static final String DEFAULT_PAGE = "0";

    @Inject
    ParticipantService participantService;

    @Inject
    JsonWebToken jwt;

    @Inject
    JWTUtils jwtUtils;

    @GET
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Participants have been returned.") })
    @Operation(summary = "Returns participant list for an engagement")
    @Counted(name = "get-participants-counted")
    @Timed(name = "get-participants-timer", unit = MetricUnits.MILLISECONDS)
    public Response getParticipants(@QueryParam("engagementUuids") Set<String> engagementUuids,
            @Parameter(description = "0 based index.") @DefaultValue(DEFAULT_PAGE) @QueryParam("page") int page,
            @DefaultValue(DEFAULT_PAGE_SIZE) @QueryParam("pageSize") int pageSize) {
        
        Response response;

        if (engagementUuids.isEmpty()) {
            response = participantService.getParticipants(page, pageSize);
        } else {
            response = participantService.getParticipants(engagementUuids, page, pageSize);
        }
        
        return response;
    }

    @GET
    @Path("/engagementUuid/{eUuid}")
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Participants have been returned for uuid.") })
    @Operation(summary = "Returns participant list for an engagement")
    @Counted(name = "get-participants-by-engagement-counted")
    @Timed(name = "get-participants-by-engagement-timer", unit = MetricUnits.MILLISECONDS)
    public Response getParticipantsForEnagementUuid(@PathParam(value = "eUuid") String engagementUuid) {
        List<EngagementUser> participants = participantService.getParticipantsForEngagement(engagementUuid);
        return Response.ok(participants).build();
    }

    @PUT
    @Path("/engagementUuid/{eUuid}")
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Participants have been returned for uuid.") })
    @Operation(summary = "Returns participant list for an engagement")
    @Counted(name = "get-participants-by-engagement-counted")
    @Timed(name = "get-participants-by-engagement-timer", unit = MetricUnits.MILLISECONDS)
    public Response getParticipantsForEngagementUuid(@PathParam(value = "eUuid") String engagementUuid,
            Set<EngagementUser> participants) {

        String email = jwtUtils.getUserEmailFromToken(jwt);
        String name = jwtUtils.getUsernameFromToken(jwt);

        participantService.updateParticipants(engagementUuid, name, email, participants);
        
        List<EngagementUser> updatedParticipants = participantService.getParticipantsForEngagement(engagementUuid);
        return Response.ok().entity(updatedParticipants).build();
    }

    @GET
    @Path("/enabled")
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Enabled participants have been returned as a map") })
    @Operation(summary = "Returns participant list for an engagement")
    @Counted(name = "get-enabled-participants-counted")
    @Timed(name = "get-enabled-participants-timer", unit = MetricUnits.MILLISECONDS)
    public Response getEnabledParticipants(@QueryParam(value = "region") List<String> region) {
        Map<String, Long> participants = participantService.getEnabledParticipants(region);
        return Response.ok(participants).build();
    }
    
    @GET
    @Path("/enabled/breakdown")
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Enabled participants have been returned as a map") })
    @Operation(summary = "Returns participant list for an engagement")
    @Counted(name = "get-enabled-participants-all-regions-counted")
    @Timed(name = "get-enabled-participants-regions-timer", unit = MetricUnits.MILLISECONDS)
    public Response getEnabledParticipantsAllRegions() {
        Map<String, Map<String, Long>> participants = participantService.getEnabledParticipantsAllRegions();
        return Response.ok(participants).build();
    }

}
