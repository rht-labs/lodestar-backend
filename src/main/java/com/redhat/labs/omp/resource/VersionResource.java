package com.redhat.labs.omp.resource;

import javax.annotation.security.PermitAll;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;

import com.redhat.labs.omp.model.Version;
import com.redhat.labs.omp.model.VersionManifest;
import com.redhat.labs.omp.model.status.VersionManifestV1;
import com.redhat.labs.omp.service.VersionService;

/**
 * Provides version information via api. Expected to come from the container in a prod env
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
    @Path("/v1/version")
    @Timed(name="versionResourceV1Timer")
    @Counted(name="versionResourceV1Counter")
    @PermitAll
    @Deprecated
    public VersionManifest getVersionV1() {
        VersionManifest vm = versionService.getVersionManifest();
        vm.addContainer(versionService.getGitApiVersion());

        return vm;
    }

    @GET
    @Path("/v2/version")
    @Timed(name="versionResourceV2Timer")
    @Counted(name="versionResourceV2Counter")
    @PermitAll
    public Version getBackendVersion() {
        return versionService.getBackendVersion();
    }

    @GET
    @PermitAll
    @Path("/v1/version/manifest")
    @Timed(name="versionManifestResourceTimer")
    @Counted(name="versionManifestResourceCounter")
    public VersionManifestV1 getVersionDetailSummary() {
        return versionService.getVersionManifestV1FromStatusClient();
    }

}
