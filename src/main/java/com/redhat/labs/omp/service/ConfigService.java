package com.redhat.labs.omp.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.redhat.labs.omp.rest.client.OMPGitLabAPIService;

@ApplicationScoped
public class ConfigService {

    @Inject
    @RestClient
    OMPGitLabAPIService gitApi;

    @ConfigProperty(name = "configFileCacheKey", defaultValue = "schema/config.yml")
    String configFileCacheKey;

    @ConfigProperty(name = "configRepositoryId", defaultValue = "9407")
    String configRepositoryId;

    public String getConfigData() {
        return gitApi.getFile("schema/config.yml", configRepositoryId).readEntity(String.class);
    }

}
