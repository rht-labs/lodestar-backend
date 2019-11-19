package com.rht_labs;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.TransportConfigCallback;
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
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.security.Principal;
import java.sql.Timestamp;

@Path("/project")
@RequestScoped
public class Project {

    @Inject
    JsonWebToken jwt;

//    @Inject
//    Git git;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @PermitAll
    public String defaultEndpoint(@Context SecurityContext ctx) {
        Principal caller =  ctx.getUserPrincipal();
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
    @Path("git")
    @Produces(MediaType.TEXT_PLAIN)
    public String readFromRepo(@Context SecurityContext ctx) throws Exception {
        // Clone repo
        File workingDir = Files.createTempDirectory("workspace").toFile();
        TransportConfigCallback transportConfigCallback = new SshTransportConfigCallback();
        Git git = Git.cloneRepository()
                .setDirectory(workingDir)
                .setTransportConfigCallback(transportConfigCallback)
                .setURI(System.getenv("GIT_REPO_URL"))
                .call();

        // Make a change
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        FileWriter file = new FileWriter(workingDir.getAbsolutePath() + "/current-timestamp");
        file.write(timestamp.toString());
        file.close();

        // Add & Commit
        git.add().addFilepattern(".").call();
        git.commit().setMessage("Update timestamp").call();

        // Push
        PushCommand pushCommand = git.push();
        pushCommand.setTransportConfigCallback(transportConfigCallback);
        pushCommand.call();

        // Return _something_ to the user.
        return "File updated with " + timestamp.toString();
    }
}