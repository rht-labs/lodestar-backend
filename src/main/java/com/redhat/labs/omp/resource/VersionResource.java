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

import com.redhat.labs.omp.model.VersionManifest;
import com.redhat.labs.omp.service.VersionService;

/**
 * Provides version information via api. Expected to come from the container in a prod env
 * @author mcanoy
 *
 */
@RequestScoped
@Path("/api/v1/version")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VersionResource {

    @Inject
    VersionService versionService;
    
    @GET
    @Timed(name="versionResourceTimer")
    @Counted(name="versionResourceCounter")
    @PermitAll
    public VersionManifest getVersion() {
        VersionManifest vm = versionService.getVersionManifest();
        vm.getContainers().add(versionService.getGitApiVersion());

        return vm;
    }
}
