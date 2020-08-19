package com.redhat.labs.omp.health;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.redhat.labs.omp.rest.client.OMPGitLabAPIService;

@Readiness
@ApplicationScoped
public class GitApiHealthCheck implements HealthCheck {

    private static final String NAME = "Git API";

    @Inject
    @RestClient
    OMPGitLabAPIService service;

    @Override
    public HealthCheckResponse call() {

        try {
            service.getVersion();
            return HealthCheckResponse.up(NAME);
        } catch (Exception e) {
            return HealthCheckResponse.down(NAME);
        }

    }

}
