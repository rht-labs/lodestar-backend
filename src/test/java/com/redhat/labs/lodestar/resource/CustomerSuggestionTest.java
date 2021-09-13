package com.redhat.labs.lodestar.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import javax.ws.rs.core.Response;

import com.redhat.labs.lodestar.rest.client.EngagementApiClient;
import io.quarkus.test.junit.mockito.InjectMock;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.redhat.labs.lodestar.utils.TokenUtils;

import io.quarkus.test.junit.QuarkusTest;
import org.mockito.Mockito;

import java.util.Collections;

@QuarkusTest
@Tag("nested")
class CustomerSuggestionTest {

	private static final String ANSWER = "Red Hat";
	private static final String SUGGESTION_URL = "/engagements/customers/suggest";

	@InjectMock
	@RestClient
	EngagementApiClient engagementApiClient;
	
	String token;	
	
	@BeforeEach
	void setUp() {
        token = TokenUtils.generateTokenString("/JwtClaimsWriter.json");
	}
	
	@Test void testSuggestionsNoSuggestionFail() {

        given()
        .when()
            .auth().oauth2(token)
        .then()
            .statusCode(400);
    }
	
	@Test void testSuggestionsHasSuggestion() {
		Mockito.when(engagementApiClient.suggest("Red")).thenReturn(
				Response.ok(Collections.singletonList("Red Hat")).build());

        given()
        .when()
            .auth().oauth2(token).queryParam("suggest", "Red").get(SUGGESTION_URL)
        .then()
            .statusCode(200).body(containsString(ANSWER));
    }
}