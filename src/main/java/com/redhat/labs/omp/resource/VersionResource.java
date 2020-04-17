package com.redhat.labs.omp.resource;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.omp.model.Version;
import com.redhat.labs.omp.model.VersionManifest;
import com.redhat.labs.omp.service.VersionService;

/**
 * Provides version information via api. Expected to come from the container in a prod env
 * @author mcanoy
 *
 */
@Path("/api/v1/version")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VersionResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(VersionResource.class);

    @ConfigProperty(name = "git.commit")
    String gitCommit;
    
    @ConfigProperty(name = "git.tag")
    String gitTag;
    
    @Inject
    VersionService versionService;
    
    @GET
    @Timed(name="versionResourceTimer")
    @Counted(name="versionResourceCounter")
    @PermitAll
    public VersionManifest getVersion() {
    	List<Version> versions = new ArrayList<>();
    	versions.add(versionService.getGitApiVersion());
    	Version version = Version.builder().application("omp-backend-container").gitCommit(gitCommit).gitTag(gitTag).build();
    	
    	LOGGER.debug(version.toString());
    	versions.add(version);
        return VersionManifest.builder().versions(versions).applicationData(versionService.getVersionManifest()).build();
    }
}
