package com.redhat.labs.omp.health;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import com.redhat.labs.omp.cache.EngagementDataCache;


@Readiness
@ApplicationScoped
public class CacheHealthCheck implements HealthCheck{

    @Inject
    EngagementDataCache residencyDataCache;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder healthCheckResponseBuilder =  HealthCheckResponse.named("Cache Connection");
        
        if (this.checkCacheConnection()) {
            healthCheckResponseBuilder.up().withData("OK", "\uD83D\uDC4D");
        } else {
            healthCheckResponseBuilder.down().withData("OK", "\uD83D\uDC4E");
        }
        
        
        return healthCheckResponseBuilder.build();
    }

    private boolean checkCacheConnection() {
        return residencyDataCache.getCacheManager().isStarted();
    }
}
