package com.redhat.labs.omp.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.omp.model.Version;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class VersionManifestConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(VersionManifestConfig.class);
    
    @ConfigProperty(name = "version.json")
    String versionJsonFile;
    
    private List<Version> versionData;

    @SuppressWarnings("serial")
	void onStart(@Observes StartupEvent event) {
        LOGGER.warn("Loading versions from {}", versionJsonFile);
        
        Path path = Paths.get(versionJsonFile);
        
        if(Files.isReadable(path)) {
            try {
            	String fileContents = new String(Files.readAllBytes(path));
            	
            	JsonbConfig config = new JsonbConfig();
                Jsonb jsonb = JsonbBuilder.create(config);
                versionData = jsonb.fromJson(fileContents, new ArrayList<Version>() {}.getClass().getGenericSuperclass());
				LOGGER.warn(versionData.toString());
			} catch (IOException e) {
				LOGGER.error("Found but unable to read file {}", versionJsonFile);
			}
        } else {
            LOGGER.warn("Unable to locate version manifest file at {}", versionJsonFile);
        }
    }
    
    public List<Version> getVersionData() {
    	return versionData;
    }
    
}
