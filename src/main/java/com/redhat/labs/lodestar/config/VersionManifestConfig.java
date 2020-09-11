package com.redhat.labs.lodestar.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.redhat.labs.lodestar.model.Version;
import com.redhat.labs.lodestar.model.VersionManifest;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;

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
    private long lastModifiedTime;

    void onStart(@Observes StartupEvent event) {
        setAppVersion();
        loadVersionData();
    }

    @Scheduled(every="10s")
    void loadVersionData() {
        
        Path path = Paths.get(versionJsonFile);
        
        if(Files.isReadable(path)) {
            if(isModified(path)) {
                LOGGER.debug("Loading versions from {}", versionJsonFile);
                try {
                    String fileContents = new String(Files.readAllBytes(path));

                    ObjectMapper om = new ObjectMapper(new YAMLFactory());
                    versionData = om.readValue(fileContents, VersionManifest.class);
                    versionData.addContainer(version);
                    LOGGER.debug(versionData.toString());
                } catch (IOException e) {
                    LOGGER.error(String.format("Found but unable to read file %s", versionJsonFile), e);
                }
            }
        } else {
            LOGGER.warn("Unable to locate version manifest file at {}. ok in dev mode.", versionJsonFile);
        }
    }
    
    /**
     * Clears data first so previous data (git-api) no longer appears
     * @return
     */
    public VersionManifest getVersionData() {
        versionData.clearAndAddContainer(version);
        return versionData;
    }

    /**
     * Gets the container data from env vars
     */
    private void setAppVersion() {
        version = Version.builder().application("lodestar-backend-container").gitCommit(gitCommit).gitTag(gitTag).version(gitCommit).build();
        LOGGER.debug("Git tag {}", version.getGitTag());
        if(version.getGitTag().equals("master") || version.getGitTag().equals("latest")) {
            version.setVersion(version.getGitTag() + "-" + version.getGitCommit());
        } else {
            version.setVersion(version.getGitTag());
        }
    }

    private boolean isModified(Path file) {
        LOGGER.trace("Checking mod for version manifest config");
        FileTime fileTime;
        try {
            fileTime = Files.getLastModifiedTime(file);
            if(fileTime.toMillis() > lastModifiedTime) {
                LOGGER.info("New version data detected for {}", versionJsonFile);
                lastModifiedTime = fileTime.toMillis();
                return true;
            }
        } catch (IOException e) {
            LOGGER.error("Unable to locate read file timestamp {}", versionJsonFile);
        }

        return false;
    }
}
