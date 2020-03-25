package com.redhat.labs.omp.cache;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.infinispan.client.Remote;
import io.quarkus.runtime.StartupEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

/**
 * A very simple facade to write the cache data to remote JDG caches.
 *
 * @author faisalmasood
 */
@ApplicationScoped
public class EngagementDataCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(EngagementDataCache.class);

	public static final String CONFIG_FILE_CACHE_KEY = "schema/config.yml";

    void onStart(@Observes StartupEvent ev) {
        if(cache == null) {
            createCache();
        }
    }

    @Inject
    protected RemoteCacheManager cacheManager;

    @Inject @Remote("omp")
    protected RemoteCache<String, String> cache;

    public RemoteCacheManager getCacheManager() {
        return cacheManager;
    }

    public String fetchConfigFile() {
        return fetch(CONFIG_FILE_CACHE_KEY);
    }

    public String fetch(String key) {
        assert (key != null);
        return cache.get(key);
    }

    public void store(String key, String value) {
        cache.put(key, value);
    }

    private void createCache() {
        LOGGER.info("Create OMP cache");
        cache = cacheManager.administration().getOrCreateCache("omp", "default");
    }
}