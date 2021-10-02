package com.redhat.labs.lodestar.resource;

import com.redhat.labs.lodestar.rest.client.ActivityApiClient;
import com.redhat.labs.lodestar.rest.client.HostingEnvironmentApiClient;
import com.redhat.labs.lodestar.utils.IntegrationTestHelper;
import com.redhat.labs.lodestar.utils.TokenUtils;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import static com.redhat.labs.lodestar.resource.EngagementResource.ACCESS_CONTROL_EXPOSE_HEADER;
import static com.redhat.labs.lodestar.resource.EngagementResource.LAST_UPDATE_HEADER;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@Tag("nested")
class EngagementResourceHeadTest extends IntegrationTestHelper {

    @InjectMock
    @RestClient
    HostingEnvironmentApiClient hostingEnvironmentApiClient;

    static String validToken = TokenUtils.generateTokenString("/JwtClaimsWriter.json");

    @Test
    void testReturnOkIfSubdomainExists() {

        String subdomain = "asuperrandomsubdomain";
        Mockito.when(hostingEnvironmentApiClient.isSubdomainValid("phony-uuid", subdomain)).thenReturn(Response.ok().build());

        given().when().auth().oauth2(validToken).head(String.format("/engagements/subdomain/%s", subdomain)).then()
                .statusCode(200);

        given().when().auth().oauth2(validToken).head(String.format("/engagements/phony-uuid/subdomain/%s", subdomain)).then()
                .statusCode(200);
    }

    @Test
    void testReturnConflictIfSubdomainExists() {

        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json");

        String subdomain = "asuperrandomsubdomain";
        Mockito.when(hostingEnvironmentApiClient.isSubdomainValid("phony-uuid", subdomain)).thenThrow(
                new WebApplicationException(409)
        );

        given().when().auth().oauth2(token).head(String.format("/engagements/subdomain/%s", subdomain)).then()
                .statusCode(409);

    }

    @Test
    void testHeadEngagementWithAuthAndRoleSuccess() {

        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json");

        Response response = Response.ok().header(LAST_UPDATE_HEADER, "last-update")
                .header(ACCESS_CONTROL_EXPOSE_HEADER, LAST_UPDATE_HEADER).build();
        Mockito.when(engagementApiClient.getEngagementHead("1234")).thenReturn(response);

        // HEAD
        given()
            .when()
                .auth()
                .oauth2(token)
                .head("/engagements/1234")
            .then()
                .statusCode(200)
                .header("last-update", notNullValue())
                .header("Access-Control-Expose-Headers", "last-update");

    }

    @Test
    void testHeadEngagementWithAuthAndRoleNptFound() {

        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json");

        Mockito.when(engagementApiClient.getEngagementHead("1234")).thenReturn(Response.status(404).build());

        // HEAD
        given()
            .when()
                .auth()
                .oauth2(token)
                .head("/engagements/1234")
            .then()
                .statusCode(404);

    }
}