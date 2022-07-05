package com.redhat.labs.lodestar.resource;

import com.redhat.labs.lodestar.model.HostingEnvironment;
import com.redhat.labs.lodestar.rest.client.HostingEnvironmentApiClient;
import com.redhat.labs.lodestar.utils.TokenUtils;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
@Tag("nested")
class HostingEnvironmentResourceTest {

    @InjectMock
    @RestClient
    HostingEnvironmentApiClient hostingEnvironmentApiClient;

    @Test
    void testGetHostingEnvironments() {

        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json");

        HostingEnvironment he = HostingEnvironment.builder().environmentName("env1").ocpCloudProviderName("provider1")
                .ocpClusterSize("small").ocpPersistentStorageSize("none").ocpSubDomain("env-one").ocpVersion("4.x.x")
                .build();
        Mockito.when(hostingEnvironmentApiClient.getHostingEnvironments(Collections.emptySet(), 0, 500))
                        .thenReturn(Response.ok(List.of(he)).build());

        given()
                .auth()
                .oauth2(token)
                .contentType(ContentType.JSON)
                .when()
                .get("engagements/hosting/environments")
                .then()
                .statusCode(200)
                .body(containsString("env1"))
                .body(containsString("env-one"));

    }

    @Test
    void testGetHostingEnvironmentsForEngagement() {

        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json");

        HostingEnvironment he = HostingEnvironment.builder().environmentName("env1").ocpCloudProviderName("provider1")
                .ocpClusterSize("small").ocpPersistentStorageSize("none").ocpSubDomain("env-one").ocpVersion("4.x.x")
                .build();
        Mockito.when(hostingEnvironmentApiClient.getHostingEnvironmentsByEngagementUuid("engagement-uuid"))
                .thenReturn(List.of(he));

        given()
                .auth()
                .oauth2(token)
                .contentType(ContentType.JSON)
                .when()
                .pathParam("engagementUuid", "engagement-uuid")
                .get("engagements/hosting/environments/engagement/{engagementUuid}")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].environment_name", equalTo("env1"))
                .body(containsString("env-one"));

    }

    @Test
    void testPutHostingEnvironmentsForEngagement() {

        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json");

        HostingEnvironment he = HostingEnvironment.builder().environmentName("env1").ocpCloudProviderName("provider1")
                .ocpClusterSize("small").ocpPersistentStorageSize("none").ocpSubDomain("env-one").ocpVersion("4.x.x")
                .build();
        List hes = List.of(he);

        Mockito.when(hostingEnvironmentApiClient.updateHostingEnvironments(Mockito.eq("engagement-uuid"), Mockito.anyList(),
                        Mockito.anyString(), Mockito.anyString()))
                .thenReturn(hes);

        given()
                .auth()
                .oauth2(token)
                .contentType(ContentType.JSON)
                .when()
                .pathParam("engagementUuid", "engagement-uuid")
                .body(hes)
                .put("engagements/hosting/environments/engagement/{engagementUuid}")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].environment_name", equalTo("env1"))
                .body(containsString("env-one"));

    }
}
