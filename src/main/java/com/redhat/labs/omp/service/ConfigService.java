package com.redhat.labs.omp.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.redhat.labs.omp.model.git.api.GitApiFile;
import com.redhat.labs.omp.rest.client.OMPGitLabAPIService;

@ApplicationScoped
public class ConfigService {

    @Inject
    @RestClient
    OMPGitLabAPIService gitApi;

    public GitApiFile getConfigData() {
        return gitApi.getConfigFile();
    }

}
