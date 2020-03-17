package com.redhat.labs.omp.resources;

import javax.annotation.security.PermitAll;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.omp.cache.ResidencyDataCache;
import com.redhat.labs.omp.service.OMPGitLabAPIService;

@Path("/config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class ConfigResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(EngagementResource.class);
	
	@Inject
    JsonWebToken jwt;

    @Inject
    @RestClient
    OMPGitLabAPIService gitApi;

    @Inject
    ResidencyDataCache engagementCache;
    
    @ConfigProperty(name = "configRepositoryId",defaultValue = "9407")
    String configRepositoryId;
	
    @GET
    @PermitAll
    public String fetchConfigData() {
        String configFile = engagementCache.fetchConfigFile();
        if(configFile == null) {
            LOGGER.info("Cache miss for config data");
            configFile = gitApi.getFile("schema/config.yml", configRepositoryId).readEntity(String.class);
        }

        return configFile;
    }
}
