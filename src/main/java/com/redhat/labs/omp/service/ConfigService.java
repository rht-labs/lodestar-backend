package com.redhat.labs.omp.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.redhat.labs.omp.rest.client.OMPGitLabAPIService;

@ApplicationScoped
public class ConfigService {

    @Inject
    @RestClient
    OMPGitLabAPIService gitApi;

    public Response getConfigData(String apiVersion) {
        Response response = Response.status(Status.NOT_FOUND).build();

        if(apiVersion == null || apiVersion.equals("v1")) {
            response = gitApi.getConfigFile();
        } else if(apiVersion.equals("v2")){
            response = gitApi.getConfigFileV2();
        }

        return response;
    }
}
