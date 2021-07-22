package com.redhat.labs.lodestar.health;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

@Liveness
@ApplicationScoped
public class BackendLivenessCheck implements HealthCheck {

    private static final String BACKEND_LIVENESS = "BACKEND LIVENESS";

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.up(BACKEND_LIVENESS);
    }

}
