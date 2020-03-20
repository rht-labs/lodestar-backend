package com.redhat.labs.mocks;


import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import com.redhat.labs.omp.cache.ResidencyDataCache;

import io.quarkus.test.Mock;

@Mock
@ApplicationScoped
public class MockResidencyDataCache extends ResidencyDataCache {
    Map<String, String> cache = new HashMap<String, String>();


    @PostConstruct
    public void init() {
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
}
