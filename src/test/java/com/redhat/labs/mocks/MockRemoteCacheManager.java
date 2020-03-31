package com.redhat.labs.mocks;

import javax.enterprise.context.ApplicationScoped;

import org.infinispan.client.hotrod.RemoteCacheManager;

import io.quarkus.test.Mock;

@Mock
@ApplicationScoped
public class MockRemoteCacheManager extends RemoteCacheManager {

	/**
	 * Setting start to false so that the hotrod client does not try
	 * to connect to a remote or local infinispan instance.
	 */

	public MockRemoteCacheManager() {
		super(false);
	}

	public MockRemoteCacheManager(boolean start) {
		super(false);
	}

	

}
