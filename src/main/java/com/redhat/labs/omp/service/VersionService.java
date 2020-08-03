package com.redhat.labs.omp.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.omp.config.VersionManifestConfig;
import com.redhat.labs.omp.model.Version;
import com.redhat.labs.omp.model.VersionManifest;
import com.redhat.labs.omp.model.status.VersionManifestV1;
import com.redhat.labs.omp.rest.client.LodeStarStatusApiClient;
import com.redhat.labs.omp.rest.client.OMPGitLabAPIService;

@ApplicationScoped
public class VersionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionService.class);

    @ConfigProperty(name = "git.commit")
    String gitCommit;

    @ConfigProperty(name = "git.tag")
    String gitTag;

    @RestClient
    @Inject
    OMPGitLabAPIService gitApiService;

    @Inject
    @RestClient
    LodeStarStatusApiClient statusApiClient;

    @Inject
    VersionManifestConfig versionManifestConfig;

    /**
     * Returns the configured {@link Version} containing the git commit and git tag.
     * 
     * @return
     */
    public Version getBackendVersion() {
        return Version.builder().gitCommit(gitCommit).gitTag(gitTag).build();
    }

    /**
     * Returns the {@link VersionManifestV1} from the LodeStar Status Service.
     * 
     * @return
     */
    public VersionManifestV1 getVersionManifestV1FromStatusClient() {
        return statusApiClient.getVersionManifestV1();
    }

    /**
     * Returns the {@link VersionManifest} from the configured ConfigMap. This
     * method is {@link Deprecated}.
     * 
     * The version manifest should be retreived using
     * getVersionManifestV1FromStatusClient().
     * 
     * @return
     */
    @Deprecated
    public VersionManifest getVersionManifest() {
        return versionManifestConfig.getVersionData();
    }

    /**
     * Retrieves the Version of the Git API using the service's version API. This
     * method is {@link Deprecated}.
     * 
     * The LodeStar Status API should be used to return the version of all LodeStar
     * components.
     * 
     * @return
     */
    public Version getGitApiVersion() {
        Version version;

        try {
            version = gitApiService.getVersion();
        } catch (WebApplicationException | ProcessingException ex) {
            LOGGER.error("Error get version", ex);
            version = Version.builder().gitTag("error-getting-version").gitCommit("error-getting-commit").build();
        }
        version.setApplication("omp-git-api-container");

        if (version.getGitTag().equals("master") || version.getGitTag().equals("latest")) {
            version.setVersion(version.getGitTag() + "-" + version.getGitCommit());
        } else {
            version.setVersion(version.getGitTag());
        }

        return version;
    }
}
