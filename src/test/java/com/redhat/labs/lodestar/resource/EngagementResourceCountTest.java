package com.redhat.labs.lodestar.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import java.util.HashMap;
import java.util.Map;

import com.redhat.labs.lodestar.rest.client.EngagementApiClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.mockito.InjectMock;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.utils.TokenUtils;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(EngagementResource.class)
@Tag("nested")
class EngagementResourceCountTest {

    @InjectMock
    @RestClient
    EngagementApiClient engagementApiClient;

    @Test
    void resourceCount() {

        String url = "/count";
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json");

        Map<Engagement.EngagementState, Integer> counts = new HashMap<>();
        counts.put(Engagement.EngagementState.UPCOMING, 16);
        counts.put(Engagement.EngagementState.ACTIVE, 8);
        counts.put(Engagement.EngagementState.TERMINATING, 15);
        counts.put(Engagement.EngagementState.PAST, 2);
        counts.put(Engagement.EngagementState.ANY, 42);

        Mockito.when(engagementApiClient.getEngagementCounts()).thenReturn(counts);

        given().when().auth().oauth2(token).get(url).then().statusCode(200).body("UPCOMING", equalTo(16))
                .body("ACTIVE", equalTo(8))
                .body("TERMINATING", equalTo(15))
                .body("PAST", equalTo(2))
                .body("ANY", equalTo(42));
    }

}
