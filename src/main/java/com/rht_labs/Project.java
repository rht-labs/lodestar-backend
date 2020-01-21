package com.rht_labs;

import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.jwt.JsonWebToken;

import javax.annotation.security.PermitAll;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

@Path("/project")
@RequestScoped
@Liveness
@Readiness
public class Project {

    @Inject
    JsonWebToken jwt;

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
    @Produces(MediaType.APPLICATION_JSON)
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
    public String fetchConfigDataFromCache(@Context SecurityContext ctx) {

        return residencyDataCache.fetchConfigFile();
    }

    @Inject
    ResidencyDataCache residencyDataCache;


}