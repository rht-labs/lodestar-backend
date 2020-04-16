package com.redhat.labs.omp.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.redhat.labs.omp.model.git.api.GitApiFile;
import com.redhat.labs.omp.rest.client.OMPGitLabAPIService;

@ApplicationScoped
public class ConfigService {

    @Inject
    @RestClient
    OMPGitLabAPIService gitApi;

    @ConfigProperty(name = "configFile", defaultValue = "schema/config.yml")
    String configFile;

    @ConfigProperty(name = "configRepositoryId", defaultValue = "9407")
    String configRepositoryId;

    public GitApiFile getConfigData() {
        return gitApi.getFile(Integer.valueOf(configRepositoryId), configFile);
    }

}
