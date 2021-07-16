package com.redhat.labs.lodestar.resource;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.redhat.labs.lodestar.model.EngagementUser;
import com.redhat.labs.lodestar.service.ParticipantService;

@RequestScoped
@Path("/engagements/participants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityScheme(securitySchemeName = "jwt", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@Tag(name = "Participants", description = "Participants in Engagements")
public class ParticipantResource {

    @Inject
    ParticipantService participantService;
    
    @GET
    @Path("/engagementUuid/{eUuid}")
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Artifacts have been returned.") })
    @Operation(summary = "Returns participant list for an engagement")
    @Counted(name = "get-participants-by-uengagement-counted")
    @Timed(name = "get-participants-by-engagement-timer", unit = MetricUnits.MILLISECONDS)
    public Response getParticipantsForEnagementUuid(@PathParam(value = "eUuid") String engagementUuid) {
        List<EngagementUser> participants = participantService.getParticiipantsForEngagement(engagementUuid);
        return Response.ok(participants).build();
    }
    
}
