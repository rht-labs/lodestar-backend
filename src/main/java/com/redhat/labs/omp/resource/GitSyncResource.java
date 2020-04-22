package com.redhat.labs.omp.resource;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;

import com.redhat.labs.omp.service.GitSyncService;

@RequestScoped
@Path("/engagements")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GitSyncResource {

    @Inject
    GitSyncService service;

    @Inject
    JsonWebToken jwt;

    @PUT
    @Path("/refresh")
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = {
            @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Git data successfully refreshed database.")})
    @Operation(summary = "Purges the database and refreshes it with data in git.")
    public Response refresh() {

        service.refreshBackedFromGit();
        return Response.ok().build();

    }

    @PUT
    @Path("/process/modified")
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = {
            @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "All modified engagement records persisted in git.")})
    @Operation(summary = "Sends all modified engagements to git to be stored.")
    public Response push() {

        service.processModifiedEngagements();
        return Response.ok().build();

    }

    @PUT
    @Path("/autosave/toggle")
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = {
            @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "The autosave feature has been toggled on or off.")})
    @Operation(summary = "Starts or stops the autosave feature, depending on the current state.")
    public Response toggle() {

        boolean value = service.toggleAutoSave();
        JsonObject model = Json.createObjectBuilder().add("autosave", value).build();
        return Response.ok().entity(model).build();

    }

}
