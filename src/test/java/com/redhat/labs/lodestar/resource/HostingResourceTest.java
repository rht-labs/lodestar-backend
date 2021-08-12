package com.redhat.labs.lodestar.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.HostingEnvOpenShfitRollup;
import com.redhat.labs.lodestar.model.HostingEnvironment;
import com.redhat.labs.lodestar.model.filter.FilterOptions;
import com.redhat.labs.lodestar.utils.IntegrationTestHelper;
import com.redhat.labs.lodestar.utils.MockUtils;
import com.redhat.labs.lodestar.utils.TokenUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@Tag("nested")
class HostingResourceTest extends IntegrationTestHelper {

    static String validToken;

    @BeforeAll
    static void setUp() throws Exception {
        validToken = TokenUtils.generateTokenString("/JwtClaimsWriter.json");
    }

    @Test
    void testGetHostingEnvironments() {

        HostingEnvironment he = MockUtils.mockHostingEnvironment("env1", "env-one");
        Response r = Response.ok(Collections.singletonList(he)).build();

        Mockito.when(hostingApiClient.getHostingEnvironments(0, 10)).thenReturn(r);

        given().queryParam("pageSize", 10).when().auth().oauth2(validToken).get("/hosting/environments").then().statusCode(200)
                .body("[0].environment_name", equalTo("env1")).body("[0].ocp_sub_domain", equalTo("env-one"));

    }

    @Test
    void testGetHostingEnvironmentByUuid() {

        HostingEnvironment he = MockUtils.mockHostingEnvironment("env1", "env-one");
        Response r = Response.ok(Collections.singletonList(he)).build();

        Mockito.when(hostingApiClient.getHostingEnvironmentsForEngagement("uuid")).thenReturn(r);

        given().pathParam("engagementUuid", "uuid").when().auth().oauth2(validToken).get("/hosting/environments/engagements/{engagementUuid}").then()
                .statusCode(200).body("[0].environment_name", equalTo("env1")).body("[0].ocp_sub_domain", equalTo("env-one"));
    }

    @Test
    void testGetVersions() {

        String version = "{ \"All\": 15, \"4.7.2\": 2, \"4.4.15\": 14 }";

        Response versionResponse = Response.ok(version).build();

        Mockito.when(hostingApiClient.getOpenShiftVersions(HostingEnvOpenShfitRollup.OCP_VERSION, Collections.emptyList()))
                .thenReturn(versionResponse);

        given().queryParam("depth", "OCP_VERSION").when().auth().oauth2(validToken).get("/hosting/environments/openshift/versions").then()
                .statusCode(200).body("All", equalTo(15)).body("'4.4.15'", equalTo(14)).body("'4.7.2'", equalTo(2));

    }

    @Test
    void testGetVersionsForRegion() {

        String version = "{ \"All\": 15, \"4.7.2\": 2, \"4.4.15\": 14 }";

        Response versionResponse = Response.ok(version).build();

        Mockito.when(hostingApiClient.getOpenShiftVersions(HostingEnvOpenShfitRollup.OCP_VERSION, Collections.emptyList()))
                .thenReturn(versionResponse);

        given().queryParam("depth", "OCP_VERSION").when().auth().oauth2(validToken).get("/hosting/environments/openshift/versions").then()
                .statusCode(200).body("All", equalTo(15)).body("'4.4.15'", equalTo(14)).body("'4.7.2'", equalTo(2));

    }

    @ParameterizedTest
    @CsvSource(value = { "200", "409" })
    void testValidSubdomain(int status) {
        Mockito.when(hostingApiClient.isSubdomainValid("uuid", "subd")).thenReturn(Response.status(status).build());

        given().pathParam("engagementUuid", "uuid").pathParam("subdomain", "subd").when().auth().oauth2(validToken).when()
                .head("/hosting/environments/subdomain/valid/{engagementUuid}/{subdomain}").then().statusCode(status);
    }

    @Test
    void testUpdateEngagementNotWriteable() {
        Map<String, List<String>> permissions = new HashMap<>();
        Engagement e = Engagement.builder().uuid("uuid").type("DO500").build();
        Mockito.when(configApiClient.getPermission()).thenReturn(permissions);
        Mockito.when(eRepository.findByUuid("uuid", new FilterOptions())).thenReturn(Optional.of(e));

        given().contentType(ContentType.JSON).body(Collections.singletonList(e)).pathParam("engagementUuid", "uuid").when().auth().oauth2(validToken)
                .put("/hosting/environments/engagements/{engagementUuid}").then().statusCode(403);

    }

    @Test
    void testUpdateEngagementWriteable() {
        Mockito.when(hostingApiClient.updateHostingEnvironments(Mockito.eq("uuid"), Mockito.anyString(), Mockito.anyString(), Mockito.anyList()))
                .thenReturn(Response.ok().build());

        Map<String, List<String>> permissions = new HashMap<>();
        permissions.put("DO500", Collections.singletonList("writer"));
        Engagement e = Engagement.builder().uuid("uuid").type("DO500").build();
        Mockito.when(configApiClient.getPermission()).thenReturn(permissions);
        Mockito.when(eRepository.findByUuid("uuid", new FilterOptions())).thenReturn(Optional.of(e));

        given().contentType(ContentType.JSON).body(Collections.singletonList(e)).pathParam("engagementUuid", "uuid").when().auth().oauth2(validToken)
                .put("/hosting/environments/engagements/{engagementUuid}").then().statusCode(200);
    }
}
