package com.redhat.labs.lodestar.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import java.util.HashMap;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.common.collect.Lists;
import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.service.EngagementService;
import com.redhat.labs.lodestar.utils.IntegrationTestHelper;
import com.redhat.labs.lodestar.utils.MockUtils;
import com.redhat.labs.lodestar.utils.TokenUtils;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@Tag("nested")
class CustomerSuggestionTest extends IntegrationTestHelper {
	private static final String ANSWER = "Red Hat";
	private static final String SUGGESTION_URL = "/engagements/customers/suggest";
	
	@Inject
	EngagementService engagementService;
	
	String token;
	
	@BeforeEach
	void setUp() throws Exception {
		HashMap<String, Long> timeClaims = new HashMap<>();
        token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);
		
		Engagement engagement = MockUtils.mockMinimumEngagement(ANSWER, "p1", "1234");
		engagement.setCustomerName(ANSWER);
		
		Mockito.when(eRepository.findCustomerSuggestions(Mockito.anyString())).thenReturn(Lists.newArrayList(engagement));

	}
	
	@Test void testSuggestionsExactSuccess() throws Exception {

        given()
        .when()
            .auth().oauth2(token).queryParam("suggest", ANSWER).get(SUGGESTION_URL)
        .then()
            .statusCode(200).body(containsString(ANSWER));
    }
	
	@Test void testSuggestionsNoSuggestionFail() throws Exception {

        given()
        .when()
            .auth().oauth2(token)
        .then()
            .statusCode(400);
    }

	@Test void testSuggestionsStartLowerSuccess() throws Exception {

        given()
        .when()
            .auth().oauth2(token).queryParam("suggest", "red").get(SUGGESTION_URL)
        .then()
            .statusCode(200).body(containsString(ANSWER));
    }
	
	@Test void testSuggestionsStartUpperSuccess() throws Exception {

        given()
        .when()
            .auth().oauth2(token).queryParam("suggest", "Red").get(SUGGESTION_URL)
        .then()
            .statusCode(200).body(containsString(ANSWER));
    }
	
	@Test void testSuggestionStartLowerThanUpperSuccess() throws Exception {

        given()
        .when()
            .auth().oauth2(token).queryParam("suggest", "rED").get(SUGGESTION_URL)
        .then()
            .statusCode(200).body(containsString(ANSWER));
	}
	
	@Test void testSuggestionsEndLowerSuccess() throws Exception {
		
        given()
        .when()
            .auth().oauth2(token).queryParam("suggest", "hat").get(SUGGESTION_URL)
        .then()
            .statusCode(200).body(containsString(ANSWER));
    }
	
	@Test void testSuggestionsEndUpperSuccess() throws Exception {

        given()
        .when()
            .auth().oauth2(token).queryParam("suggest", "haT").get(SUGGESTION_URL)
        .then()
            .statusCode(200).body(containsString(ANSWER));
    }
	
	@Test void testSuggestionEndLowerThanUpperSuccess() throws Exception {
 
        given()
        .when()
            .auth().oauth2(token).queryParam("suggest", "hAT").get(SUGGESTION_URL)
        .then()
            .statusCode(200).body(containsString(ANSWER));
	}
	
	@Test void testSuggestionsMidLowerSuccess() throws Exception {

        given()
        .when()
            .auth().oauth2(token).queryParam("suggest", "ed").get(SUGGESTION_URL)
        .then()
            .statusCode(200).body(containsString(ANSWER));
    }
	
	@Test void testSuggestionsMidUpperSuccess() throws Exception {

        given()
        .when()
            .auth().oauth2(token).queryParam("suggest", "ED").get(SUGGESTION_URL)
        .then()
            .statusCode(200).body(containsString(ANSWER));
    }
	
	@Test void testSuggestionMidLowerThanUpperSuccess() throws Exception {
 
        given()
        .when()
            .auth().oauth2(token).queryParam("suggest", "eD").get(SUGGESTION_URL)
        .then()
            .statusCode(200).body(containsString(ANSWER));
	}
}