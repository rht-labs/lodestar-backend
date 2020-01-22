package com.rht_labs;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

/**
 * A very simple facade to write the cache data to remote JDG caches.
 *
 * @author faisalmasood
 */
@Singleton
public class ResidencyDataCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResidencyDataCache.class);

    @ConfigProperty(name = "cacheServerName", defaultValue = "127.0.0.1")
    protected String cacheServerName;

    @PostConstruct
    public void init() {
        org.infinispan.client.hotrod.configuration.ConfigurationBuilder cb
                = new org.infinispan.client.hotrod.configuration.ConfigurationBuilder();
        cb.marshaller(new org.infinispan.commons.marshall.JavaSerializationMarshaller())
                .addJavaSerialWhiteList("com.redhat.labs.cache.*")
                .statistics()
                .enable()
                .jmxDomain("org.infinispan")
                .addServer()
                .host(cacheServerName)
                .port(11222)
        .security()
        .authentication()
                .saslMechanism("PLAINTEXT")
                .username("omp").password("omp");

        this.remoteCacheManager = new RemoteCacheManager(cb.build());
        LOGGER.info("Trying to get the cache");
        this.cache = remoteCacheManager.getCache();
    }

    private RemoteCache<String, String> cache;

    private RemoteCacheManager remoteCacheManager;

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

    public static final String CONFIG_FILE_CACHE_KEY = "configFile";
}

