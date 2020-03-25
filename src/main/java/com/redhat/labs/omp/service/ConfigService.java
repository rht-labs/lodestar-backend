package com.redhat.labs.omp.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.omp.cache.EngagementDataCache;
import com.redhat.labs.omp.rest.client.OMPGitLabAPIService;

@ApplicationScoped
public class ConfigService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigService.class);

	@Inject
	@RestClient
	OMPGitLabAPIService gitApi;

	@Inject
	EngagementDataCache engagementCache;

	@ConfigProperty(name = "configRepositoryId", defaultValue = "9407")
	String configRepositoryId;

	public String getConfigData() {

		String configFile = engagementCache.fetchConfigFile();
		if (configFile == null) {
			LOGGER.info("Cache miss for config data");
			configFile = gitApi.getFile("schema/config.yml", configRepositoryId).readEntity(String.class);
		}

		return configFile;

	}

}
