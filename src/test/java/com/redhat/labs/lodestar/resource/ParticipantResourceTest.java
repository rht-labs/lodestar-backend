package com.redhat.labs.lodestar.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.timeout;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.EngagementUser;
import com.redhat.labs.lodestar.model.filter.FilterOptions;
import com.redhat.labs.lodestar.utils.IntegrationTestHelper;
import com.redhat.labs.lodestar.utils.TokenUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@Tag("nested")
class ParticipantResourceTest extends IntegrationTestHelper {

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

    @Test
    void testGetEnagements() {
        List<EngagementUser> participants = Collections.singletonList(EngagementUser.builder().uuid("11").email("particip@nt.com").build());
        Mockito.when(participantClient.getParticipants(null, 0, 100)).thenReturn(Response.ok(participants).build());

        given().when().auth().oauth2(validToken).get("/engagements/participants").then().statusCode(200).body("[0].uuid", equalTo("11"))
                .body("[0].email", equalTo("particip@nt.com"));

        Mockito.verify(participantClient, timeout(1000)).getParticipants(null, 0, 100);
    }

    @Test
    void testGetEnagementsByUuids() {
        List<EngagementUser> participants = Collections.singletonList(EngagementUser.builder().uuid("11").email("particip@nt.com").build());

        Set<String> euuids = new HashSet<>();
        euuids.add("euuid");

        Mockito.when(participantClient.getParticipants(euuids, 0, 100)).thenReturn(Response.ok(participants).build());

        given().queryParam("engagementUuids", "euuid").when().auth().oauth2(validToken).get("/engagements/participants").then().statusCode(200)
                .body("[0].uuid", equalTo("11")).body("[0].email", equalTo("particip@nt.com"));

        Mockito.verify(participantClient, timeout(1000)).getParticipants(euuids, 0, 100);
    }
    
    @Test
    void testGetEnabled() {
        Map<String, Long> enabled = new HashMap<>();
        enabled.put("Red Hat", 1000000000L);
        enabled.put("Others", 6L);
        enabled.put("All", 1000000006L);

        Mockito.when(participantClient.getEnabledParticipants(Collections.emptyList())).thenReturn(enabled);

        given().when().auth().oauth2(validToken)
                .get("/engagements/participants/enabled").then().statusCode(200).body("'Red Hat'", equalTo(1000000000))
                .body("Others", equalTo(6)).body("All", equalTo(1000000006));

        Mockito.verify(participantClient, timeout(1000)).getEnabledParticipants(Collections.emptyList());
    }
    
    @Test
    void testGetEnabledBreakdown() {
        Map<String,Map<String, Long>> doubleMap = new HashMap<>();
        Map<String, Long> enabled = new HashMap<>();
        enabled.put("Red Hat", 1000000000L);
        enabled.put("Others", 6L);
        enabled.put("All", 1000000006L);
        doubleMap.put("All", enabled);

        Mockito.when(participantClient.getEnabledParticipantsAllRegions()).thenReturn(doubleMap);

        given().when().auth().oauth2(validToken)
                .get("/engagements/participants/enabled/breakdown").then().statusCode(200).body("size()", equalTo(1)).body("All.'Red Hat'", equalTo(1000000000))
                .body("All.Others", equalTo(6)).body("All.All", equalTo(1000000006));

        Mockito.verify(participantClient, timeout(1000)).getEnabledParticipantsAllRegions();
    }
    
    @Test
    void testUpdateParticipantsByEnagementUuid() {
        Engagement engagement = Engagement.builder().uuid("euuid").region("na").build();
        Mockito.when(eRepository.findByUuid("euuid", new FilterOptions())).thenReturn(Optional.of(engagement));
        
        List<EngagementUser> participants = Collections
                .singletonList(EngagementUser.builder().uuid("11").email("particip@nt.com").build());
        given().contentType(ContentType.JSON).body(participants).when().auth().oauth2(validToken).put("/engagements/participants/engagementUuid/euuid").then().statusCode(200);
    }
}
