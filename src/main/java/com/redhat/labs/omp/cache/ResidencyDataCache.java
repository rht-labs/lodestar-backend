package com.redhat.labs.omp.cache;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;

import io.quarkus.infinispan.client.Remote;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * A very simple facade to write the cache data to remote JDG caches.
 *
 * @author faisalmasood
 */
@ApplicationScoped
public class ResidencyDataCache {

    @Inject
    protected RemoteCacheManager cacheManager;

    public RemoteCacheManager getCacheManager() {
		return cacheManager;
	}

    @Inject @Remote("omp")
    private RemoteCache<String, String> cache;

    @ConfigProperty(name = "cacheServerName", defaultValue = "127.0.0.1")
    protected String cacheServerName;

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

    public static final String CONFIG_FILE_CACHE_KEY = "schema/config.yml";
}

