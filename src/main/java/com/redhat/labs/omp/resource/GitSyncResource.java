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
    public Response refresh() {

        service.refreshBackedFromGit();
        return Response.ok().build();

    }

    @PUT
    @Path("/process/modified")
    public Response push() {

        service.processModifiedEngagements();
        return Response.ok().build();

    }

    @PUT
    @Path("/autosave/toggle")
    public Response toggle() {

        boolean value = service.toggleAutoSave();
        JsonObject model = Json.createObjectBuilder().add("autosave", value).build();
        return Response.ok().entity(model).build();

    }

}
