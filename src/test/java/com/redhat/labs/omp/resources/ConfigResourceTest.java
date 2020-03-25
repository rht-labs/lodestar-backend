package com.redhat.labs.omp.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.util.HashMap;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.labs.omp.cache.EngagementDataCache;
import com.redhat.labs.utils.TokenUtils;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ConfigResourceTest {
	
	@Inject
	EngagementDataCache cache;

	/*
	 * 
	 * SCENARIOS:
	 * 
	 * SUCCESS -
	 *   token supplied, has correct role, in cache
	 *   token supplied, has correct role, not in cache, git api service returns 201
	 * 
	 * FAIL - 
	 *   token has wrong role - 403
	 *   TODO: The following need to be tested, but no hook to specify return from mock git api service bean
	 *   config not in cache, call git api and config not returned - fail?
	 *   config not in cache, call git api and get error response
	 *   config not in cache, call git api and get network error
	 * 
	 */

	@Test
	public void testGetConfigTokenHasWrongRole() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

		given()
			.when()
				.auth()
					.oauth2(token)
				.get("/config")
			.then()
				.statusCode(403);
		
	}

	@Test
	public void testGetConfigNotInCacheIsInGitRepo() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsReader.json", timeClaims);

		given()
			.when()
				.auth()
					.oauth2(token)
				.get("/config")
			.then()
				.statusCode(200)
				.body("emoji", is("\uD83E\uDD8A"));
		
	}

	@Test
	public void testGetConfigInCache() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsReader.json", timeClaims);

		// insert into cache
		cache.store(EngagementDataCache.CONFIG_FILE_CACHE_KEY, "{\"emoji\":\"\uD83E\uDD8A\"}");

		given()
			.when()
				.auth()
					.oauth2(token)
				.get("/config")
			.then()
				.statusCode(200)
				.body("emoji", is("\uD83E\uDD8A"));

	}

	

}
