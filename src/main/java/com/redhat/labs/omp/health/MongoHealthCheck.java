package com.redhat.labs.omp.health;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.omp.service.EngagementService;

@Readiness
@ApplicationScoped
public class MongoHealthCheck implements HealthCheck {

    @Inject
    EngagementService service;

    @Override
    public HealthCheckResponse call() {

        HealthCheckResponseBuilder healthCheckResponseBuilder = HealthCheckResponse.named("Mongo Connection");

        try {
            Engagement.count();
            healthCheckResponseBuilder.up().withData("OK", "\uD83D\uDC4D");
        } catch (Exception e) {
            healthCheckResponseBuilder.down().withData("OK", "\uD83D\uDC4E");
        }

        return healthCheckResponseBuilder.build();

    }

}
