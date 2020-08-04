package com.redhat.labs.omp.resource;

import javax.annotation.security.PermitAll;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.openapi.annotations.Operation;

import com.redhat.labs.omp.model.Version;
import com.redhat.labs.omp.model.VersionManifest;
import com.redhat.labs.omp.model.status.VersionManifestV1;
import com.redhat.labs.omp.service.VersionService;

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
    @Operation(summary = "Returns the git commit/tag data for the LodeStar Backend if Accept-version v2 or missing.  Returns version 1 of the manifest if v1 supplied.  Otherwise, 400 returned.")
    public Response getVersion(@HeaderParam(value = "Accept-version") String apiVersion) {

        if (null == apiVersion || apiVersion.equals("v2")) {
            Version version = versionService.getBackendVersion();
            return Response.ok(version).build();
        } else if (apiVersion.equals("v1")) {
            @SuppressWarnings("deprecation")
            VersionManifest vm = versionService.getVersionManifest();
            vm.addContainer(versionService.getGitApiVersion());
            return Response.ok(vm).build();
        } else {
            return Response.status(400).build();
        }

    }

    @GET
    @Path("/v1/version")
    @Timed(name = "versionResourceV1Timer")
    @Counted(name = "versionResourceV1Counter")
    @PermitAll
    @Deprecated
    @Operation(deprecated = true, summary = "Returns version 1 of the manifest.")
    public VersionManifest getVersionV1() {
        VersionManifest vm = versionService.getVersionManifest();
        vm.addContainer(versionService.getGitApiVersion());

        return vm;
    }

    @GET
    @PermitAll
    @Path("/version/manifest")
    @Timed(name = "versionManifestResourceTimer")
    @Counted(name = "versionManifestResourceCounter")
    @Operation(summary = "Returns the Version Manifest from LodeStar Status")
    public VersionManifestV1 getStatusVersionManifest() {
        return versionService.getVersionManifestV1FromStatusClient();
    }

}
