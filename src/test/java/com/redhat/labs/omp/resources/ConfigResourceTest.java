package com.redhat.labs.omp.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.labs.omp.cache.EngagementDataCache;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ConfigResourceTest {
	
	@Inject
	private EngagementDataCache cache;
	
	// TODO:  Need a way to test the negative scenarios
	// config not in cache, call git api and config not returned - fail?
	// config not in cache, call git api and get error response
	// config not in cache, call git api and get network error

	// SUCCESS Scenarios
	// config not in cache, call git api and config returned - success
	// config in cache, returns config

	@Test
	public void testGetConfigNotInCacheIsInGitRepo() {

		given()
			.when()
				.get("/config")
			.then()
				.statusCode(200)
				.body("emoji", is("\uD83E\uDD8A"));
		
	}

	@Test
	public void testGetConfigInCache() {

		// insert into cache
		cache.store(EngagementDataCache.CONFIG_FILE_CACHE_KEY, "{\"emoji\":\"\uD83E\uDD8A\"}");

		given()
			.when()
				.get("/config")
			.then()
				.statusCode(200)
				.body("emoji", is("\uD83E\uDD8A"));

	}

	

}
