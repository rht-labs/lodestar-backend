package com.redhat.labs.lodestar.resource;

import javax.annotation.security.PermitAll;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.openapi.annotations.Operation;

import com.redhat.labs.lodestar.model.status.VersionManifest;
import com.redhat.labs.lodestar.service.VersionService;

/**
 * Provides version information via api. Expected to come from the container in
 * a prod env
 * 
 * @author mcanoy
 *
 */
@RequestScoped
@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VersionResource {

    @Inject
    VersionService versionService;

    @GET
    @PermitAll
    @Path("/version")
    @Timed(name = "versionResourceTimer")
    @Counted(name = "versionResourceCounter")
    @Operation(summary = "Returns the git commit/tag data for the LodeStar Backend.")
    public Response getVersion() {
            return Response.ok(versionService.getBackendVersion()).build();
    }

    @GET
    @PermitAll
    @Path("/version/manifest")
    @Timed(name = "versionManifestResourceTimer")
    @Counted(name = "versionManifestResourceCounter")
    @Operation(summary = "Returns the Version Manifest from LodeStar Status")
    public VersionManifest getStatusVersionManifest() {
        return versionService.getVersionManifestV1FromStatusClient();
    }

}
