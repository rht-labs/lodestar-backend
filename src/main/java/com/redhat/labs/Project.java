package com.redhat.labs;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.annotation.security.PermitAll;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

@Path("/project")
@Liveness
@Readiness
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class Project {
    @Inject
    JsonWebToken jwt;

    @Inject
    @RestClient
    OMPGitLabAPIService ompGitLabAPIService;

    @ConfigProperty(name = "configRepositoryId",defaultValue = "9407")
    String configRepositoryId;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @PermitAll
    public String defaultEndpoint(@Context SecurityContext ctx) {
        Principal caller = ctx.getUserPrincipal();
        String name = caller == null ? "anonymous" : caller.getName();
        String helloReply = String.format("hello + %s, isSecure: %s, authScheme: %s", name, ctx.isSecure(), ctx.getAuthenticationScheme());
        return helloReply;
    }

    @GET
    @Path("open")
    @PermitAll
    public String openEndpoint(@Context SecurityContext ctx) {
        return Json.createObjectBuilder().add("hello", "world").build().toString();
    }

    @GET
    @Path("secure")
    @Produces(MediaType.TEXT_PLAIN)
    public String securedEndpoint(@Context SecurityContext ctx) {
        return jwt.getName();
    }

    @GET
    @Path("config")
    @Produces(MediaType.TEXT_PLAIN)
    @PermitAll
    public String fetchConfigDataFromCache(@Context SecurityContext ctx) {
    //TODO - 🤠 Make this call to cache and not to the GitLab Api thingy to get file directly
        return ompGitLabAPIService.getFile("schema%2Fconfig.yml", configRepositoryId).readEntity(String.class);
    }

    @Inject
    ResidencyDataCache residencyDataCache;


}