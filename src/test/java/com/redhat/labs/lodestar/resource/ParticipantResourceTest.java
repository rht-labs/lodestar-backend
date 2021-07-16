package com.redhat.labs.lodestar.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.timeout;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.redhat.labs.lodestar.model.EngagementUser;
import com.redhat.labs.lodestar.utils.IntegrationTestHelper;
import com.redhat.labs.lodestar.utils.TokenUtils;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@Tag("nested")
public class ParticipantResourceTest extends IntegrationTestHelper {

    static String validToken;

    @BeforeAll
    static void setUp() throws Exception {
        HashMap<String, Long> timeClaims = new HashMap<>();
        validToken = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);
    }

    @Test
    void testFetchActivityByUuid() {
        List<EngagementUser> participants = Collections
                .singletonList(EngagementUser.builder().uuid("11").email("particip@nt.com").build());
        Mockito.when(participantClient.getParticipantsForEngagement("1")).thenReturn(participants);

        given().when().auth().oauth2(validToken).get("/engagements/participants/engagementUuid/1").then()
                .statusCode(200).body("[0].uuid", equalTo("11")).body("[0].email", equalTo("particip@nt.com"));

        Mockito.verify(participantClient, timeout(1000)).getParticipantsForEngagement("1");
    }
}
