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
import com.redhat.labs.omp.model.VersionDetail;
import com.redhat.labs.omp.model.VersionManifest;
import com.redhat.labs.omp.model.VersionSummary;
import com.redhat.labs.omp.rest.client.OMPGitLabAPIService;

@ApplicationScoped
public class VersionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(VersionService.class);

    @ConfigProperty(name = "version.application.key")
    String versionApplicationKey;

    @RestClient
    @Inject
    OMPGitLabAPIService gitApiService;

    @Inject
    VersionManifestConfig versionManifestConfig;

    public VersionManifest getVersionManifest() {
        return versionManifestConfig.getVersionData();
    }

    public VersionSummary getVersionSummary() {

        VersionSummary summary = new VersionSummary();
        VersionManifest versionManifest = versionManifestConfig.getVersionData();

        versionManifest.getApplications().stream().forEach(v -> {

            VersionDetail version = VersionDetail.fromVersionV1(v);
            if(version.getName().equalsIgnoreCase(versionApplicationKey)) {
                summary.setMainVersion(version);
            } else {
                summary.getComponentVersions().add(version);
            }

        });

        return summary;

    }

    public Version getGitApiVersion() {
        Version version;

        try {
            version = gitApiService.getVersion();
        } catch (WebApplicationException | ProcessingException ex) {
            LOGGER.error("Error get version", ex);
            version = Version.builder().gitTag("error-getting-version").gitCommit("error-getting-commit").build();
        }
        version.setApplication("omp-git-api-container");
        
        if(version.getGitTag().equals("master") || version.getGitTag().equals("latest")) {
            version.setVersion(version.getGitTag() + "-" + version.getGitCommit());
        } else {
            version.setVersion(version.getGitTag());
        }

        return version;
    }
}
