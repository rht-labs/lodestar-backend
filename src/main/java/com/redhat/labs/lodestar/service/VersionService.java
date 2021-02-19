package com.redhat.labs.lodestar.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.redhat.labs.lodestar.model.ApplicationVersion;
import com.redhat.labs.lodestar.model.status.VersionManifest;
import com.redhat.labs.lodestar.rest.client.LodeStarStatusApiClient;

@ApplicationScoped
public class VersionService {

    @ConfigProperty(name = "git.commit")
    String gitCommit;

    @ConfigProperty(name = "git.tag")
    String gitTag;

    @Inject
    @RestClient
    LodeStarStatusApiClient statusApiClient;

    /**
     * Returns the configured {@link ApplicationVersion} containing the git commit and git tag.
     * 
     * @return
     */
    public ApplicationVersion getBackendVersion() {
        return ApplicationVersion.builder().gitCommit(gitCommit).gitTag(gitTag).build();
    }

    /**
     * Returns the {@link VersionManifest} from the LodeStar Status Service.
     * 
     * @return
     */
    public VersionManifest getVersionManifestV1FromStatusClient() {
        return statusApiClient.getVersionManifestV1();
    }

}
