package com.redhat.labs.lodestar.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.redhat.labs.lodestar.rest.client.LodeStarGitLabAPIService;

@ApplicationScoped
public class ConfigService {

    @Inject
    @RestClient
    LodeStarGitLabAPIService gitApi;

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
