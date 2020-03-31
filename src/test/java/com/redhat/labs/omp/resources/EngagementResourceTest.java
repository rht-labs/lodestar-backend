package com.redhat.labs.omp.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.HashMap;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.PropertyNamingStrategy;

import org.junit.jupiter.api.Test;

import com.redhat.labs.mocks.MockOMPGitLabAPIService.SCENARIO;
import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.utils.TokenUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class EngagementResourceTest {

	JsonbConfig config = new JsonbConfig()
			.withPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CASE_WITH_UNDERSCORES);
	Jsonb jsonb = JsonbBuilder.create(config);

	/*
	 * 
	 * SCENARIOS:
	 * 
	 * SUCCESS -
	 *   token supplied, has correct role, git api service returns 201
	 * 
	 * FAIL - 
	 *   token has wrong role - 403
	 *   token has role and 500 back from git service
	 *   token has role and runtime exception back from git service
	 * 
	 */

	@Test
	public void testPostEngagementWithWrongRole() throws Exception {

		HashMap<String, Long> timeClaims = new HashMap<>();
		String token = TokenUtils.generateTokenString("/JwtClaimsReader.json", timeClaims);

		String body = toJsonString(mockEngagement(SCENARIO.SUCCESS));

		given()
			.when()
				.auth()
					.oauth2(token)
				.body(body)
				.contentType(ContentType.JSON)
				.post("/engagements")
			.then()
				.statusCode(403);	

	}

	@Test
	public void testPostEngagementWithAuthAndRoleSuccess() throws Exception {

		HashMap<String, Long> timeClaims = new HashMap<>();
		String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

		String body = toJsonString(mockEngagement(SCENARIO.SUCCESS));

		given()
		.when()
			.auth()
				.oauth2(token)
			.body(body)
			.contentType(ContentType.JSON)
			.post("/engagements")
		.then()
			.statusCode(201)
			.header("Location", equalTo("http://localhost:8081/engagements/12338"));

	}

	@Test
	public void testPostEngagementWithAuthAndRoleGitApiError() throws Exception {

		HashMap<String, Long> timeClaims = new HashMap<>();
		String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

		String body = toJsonString(mockEngagement(SCENARIO.SERVER_ERROR));

		given()
		.when()
			.auth()
				.oauth2(token)
			.body(body)
			.contentType(ContentType.JSON)
			.post("/engagements")
		.then()
			.statusCode(500);

	}

	// TODO:  Runtime Exceptions should be mapped to something more useful than a 500

	@Test
	public void testPostEngagementWithAuthAndRoleGitApiRuntimeException() throws Exception {

		HashMap<String, Long> timeClaims = new HashMap<>();
		String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

		String body = toJsonString(mockEngagement(SCENARIO.RUNTIME_EXCEPTION));

		given()
		.when()
			.auth()
				.oauth2(token)
			.body(body)
			.contentType(ContentType.JSON)
			.post("/engagements")
		.then()
			.statusCode(500);

	}

	private Engagement mockEngagement(SCENARIO scenario) {

		Engagement engagement = Engagement.builder().customerName(scenario.name()).projectName("TestProject")
				.description("Test Description").location("Raleigh, NC").startDate("20170501").endDate("20170708")
				.archiveDate("20170930").engagementLeadName("Mister Lead").engagementLeadEmail("mister@lead.com")
				.technicalLeadName("Mister Techlead").technicalLeadEmail("mister@techlead.com")
				.customerContactName("Customer Contact").customerContactEmail("customer@contact.com")
				.ocpCloudProviderName("GCP").ocpCloudProviderRegion("West").ocpVersion("v4.2")
				.ocpSubDomain("jello").ocpPersistentStorageSize("50GB").ocpClusterSize("medium")
				.build();

		return engagement;

	}

	private String toJsonString(Engagement engagement) {
		return jsonb.toJson(engagement);
	}

}
