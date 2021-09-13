package com.redhat.labs.lodestar.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.timeout;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;

import com.redhat.labs.lodestar.rest.client.EngagementApiClient;
import com.redhat.labs.lodestar.rest.client.ParticipantApiClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.mockito.InjectMock;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.EngagementUser;
import com.redhat.labs.lodestar.utils.TokenUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@TestHTTPEndpoint(ParticipantResource.class)
@Tag("nested")
class ParticipantResourceTest {

    @InjectMock
    @RestClient
    ParticipantApiClient participantApiClient;

    @InjectMock
    @RestClient
    EngagementApiClient engagementApiClient;

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
        Mockito.when(participantApiClient.getParticipantsForEngagement("1")).thenReturn(participants);

        given().when().auth().oauth2(validToken).get("/engagementUuid/1").then()
                .statusCode(200).body("[0].uuid", equalTo("11")).body("[0].email", equalTo("particip@nt.com"));

        Mockito.verify(participantApiClient, timeout(1000)).getParticipantsForEngagement("1");
    }

    @Test
    void testGetEnagements() {
        List<EngagementUser> participants = Collections.singletonList(EngagementUser.builder().uuid("11").email("particip@nt.com").build());
        Mockito.when(participantApiClient.getParticipants(null, 0, 100)).thenReturn(Response.ok(participants).build());

        given().when().auth().oauth2(validToken).get().then().statusCode(200).body("[0].uuid", equalTo("11"))
                .body("[0].email", equalTo("particip@nt.com"));

        Mockito.verify(participantApiClient, timeout(1000)).getParticipants(null, 0, 100);
    }

    @Test
    void testGetEngagementsByUuids() {
        List<EngagementUser> participants = Collections.singletonList(EngagementUser.builder().uuid("11").email("particip@nt.com").build());

        Set<String> euuids = new HashSet<>();
        euuids.add("euuid");

        Mockito.when(participantApiClient.getParticipants(euuids, 0, 100)).thenReturn(Response.ok(participants).build());

        given().queryParam("engagementUuids", "euuid").when().auth().oauth2(validToken).get().then().statusCode(200)
                .body("[0].uuid", equalTo("11")).body("[0].email", equalTo("particip@nt.com"));

        Mockito.verify(participantApiClient, timeout(1000)).getParticipants(euuids, 0, 100);
    }
    
    @Test
    void testGetEnabled() {
        Map<String, Long> enabled = new HashMap<>();
        enabled.put("Red Hat", 1000000000L);
        enabled.put("Others", 6L);
        enabled.put("All", 1000000006L);

        Mockito.when(participantApiClient.getEnabledParticipants(Collections.emptyList())).thenReturn(enabled);

        given().when().auth().oauth2(validToken)
                .get("/enabled").then().statusCode(200).body("'Red Hat'", equalTo(1000000000))
                .body("Others", equalTo(6)).body("All", equalTo(1000000006));

        Mockito.verify(participantApiClient, timeout(1000)).getEnabledParticipants(Collections.emptyList());
    }
    
    @Test
    void testGetEnabledBreakdown() {
        Map<String,Map<String, Long>> doubleMap = new HashMap<>();
        Map<String, Long> enabled = new HashMap<>();
        enabled.put("Red Hat", 1000000000L);
        enabled.put("Others", 6L);
        enabled.put("All", 1000000006L);
        doubleMap.put("All", enabled);

        Mockito.when(participantApiClient.getEnabledParticipantsAllRegions()).thenReturn(doubleMap);

        given().when().auth().oauth2(validToken)
                .get("/enabled/breakdown").then().statusCode(200).body("size()", equalTo(1)).body("All.'Red Hat'", equalTo(1000000000))
                .body("All.Others", equalTo(6)).body("All.All", equalTo(1000000006));

        Mockito.verify(participantApiClient, timeout(1000)).getEnabledParticipantsAllRegions();
    }
    
    @Test
    void testUpdateParticipantsByEngagementUuid() {
        String uuid = "euuid";
        Engagement engagement = Engagement.builder().uuid(uuid).region("na").build();
        Mockito.when(engagementApiClient.getEngagement(uuid)).thenReturn(engagement);

        List<EngagementUser> participants = Collections
                .singletonList(EngagementUser.builder().uuid("11").email("particip@nt.com").build());

        Mockito.when(participantApiClient.getParticipantsForEngagement(uuid)).thenReturn(participants);

        given().contentType(ContentType.JSON).body(participants).when().auth().oauth2(validToken)
                .put("/engagementUuid/euuid").then().statusCode(200);
    }
}
