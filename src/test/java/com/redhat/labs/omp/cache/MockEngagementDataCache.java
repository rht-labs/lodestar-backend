package com.redhat.labs.omp.cache;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;

import com.redhat.labs.omp.cache.EngagementDataCache;

import io.quarkus.infinispan.client.Remote;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.Mock;

@Mock
@ApplicationScoped
public class MockEngagementDataCache extends EngagementDataCache {

	Map<String, String> cache = new HashMap<String, String>();
	
	@PostConstruct
	public void init() {
	}

	@Override
	void onStart(StartupEvent ev) {
		// do nothing
	}

	@Override
	public String fetchConfigFile() {
		return fetch(CONFIG_FILE_CACHE_KEY);
	}

	@Override
	public String fetch(String key) {
		assert (key != null);
		return cache.get(key);
	}

	@Override
	public void store(String key, String value) {
		cache.put(key, value);
	}

}
