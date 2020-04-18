package com.redhat.labs.omp.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.redhat.labs.omp.model.Version;
import com.redhat.labs.omp.model.VersionManifest;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class VersionManifestConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(VersionManifestConfig.class);
    
    @ConfigProperty(name = "version.yml")
    String versionJsonFile;

    @ConfigProperty(name = "git.commit")
    String gitCommit;

    @ConfigProperty(name = "git.tag")
    String gitTag;
    
    private VersionManifest versionData = new VersionManifest();
    private Version version;

    void onStart(@Observes StartupEvent event) {
        LOGGER.warn("Loading versions from {}", versionJsonFile);
        
        Path path = Paths.get(versionJsonFile);
        
        if(Files.isReadable(path)) {
            try {
                String fileContents = new String(Files.readAllBytes(path));

                ObjectMapper om = new ObjectMapper(new YAMLFactory());
                versionData = om.readValue(fileContents, VersionManifest.class);
                LOGGER.warn(versionData.toString());
            } catch (IOException e) {
                LOGGER.error(String.format("Found but unable to read file %s", versionJsonFile), e);
            }
        } else {
            LOGGER.warn("Unable to locate version manifest file at {}", versionJsonFile);
        }

        setAppVersion();
    }
    
    public VersionManifest getVersionData() {
        versionData.getContainers().clear();
        versionData.getContainers().add(version);
        return versionData;
    }

    private void setAppVersion() {
        versionData.setContainers(new ArrayList<Version>());
        version = Version.builder().application("omp-backend-container").gitCommit(gitCommit).gitTag(gitTag).version(gitCommit).build();
        if(version.getGitTag().startsWith("v")) {
            version.setVersion(gitTag);
        }
    }
}
