package com.redhat.labs.omp.health;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;


@Readiness
@ApplicationScoped
public class CacheHealthCheck implements HealthCheck{

    @Override
    public HealthCheckResponse call() {

        HealthCheckResponseBuilder healthCheckResponseBuilder =  HealthCheckResponse.named("Cache Connection");

        // TODO:  This should check if mongo and our messaging system is up or down
        return healthCheckResponseBuilder.build();
    }

}
