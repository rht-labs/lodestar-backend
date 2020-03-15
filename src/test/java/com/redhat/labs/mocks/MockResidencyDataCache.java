package com.redhat.labs.mocks;


import io.quarkus.test.Mock;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import com.redhat.labs.omp.cache.ResidencyDataCache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Mock
@Singleton
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
