package com.redhat.labs.lodestar.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.json.bind.Jsonb;

import org.junit.jupiter.api.Test;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.rest.client.MockLodeStarGitLabAPIService.SCENARIO;
import com.redhat.labs.lodestar.utils.EmbeddedMongoTest;
import com.redhat.labs.lodestar.utils.TokenUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@EmbeddedMongoTest
@QuarkusTest
public class GitSyncResourceTest {

    @Inject
    Jsonb quarkusJsonb;

    /*
     * Process Modified
     * Positive:
     *  - 1 create, 1 update, 1 delete in mongo --> call git api service success
     *  - 1 create, then update --> call git api success
     * Negative:
     *  - git api throws exception
     * 
     */
    @Test
    public void testPutEngagementProcessModifiedWithAuthAndRoleSuccess() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = mockEngagement();
        engagement.setDescription(SCENARIO.SUCCESS.getValue());

        String body = quarkusJsonb.toJson(engagement);

        // POST engagement - create
        Response response = given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .post("/engagements");

        assertEquals(201, response.getStatusCode());

        String responseBody = response.getBody().asString();
        Engagement created = quarkusJsonb.fromJson(responseBody, Engagement.class);

        assertEquals(engagement.getCustomerName(), created.getCustomerName());
        assertEquals(engagement.getProjectName(), created.getProjectName());
        assertNull(created.getProjectId());
        assertNotNull(created.getLastUpdate());

        // process created
        given()
            .when()
                .auth()
                .oauth2(token)
                .contentType(ContentType.JSON)
                .put("/engagements/process/modified")
            .then()
                .statusCode(200);

        // make sure the async processes finish
        TimeUnit.SECONDS.sleep(1);

        // update description
        created.setDescription("updated");
        body = quarkusJsonb.toJson(created);

        // PUT engagement - update
        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/customers/" + engagement.getCustomerName() + "/projects/" + engagement.getProjectName())
            .then()
                .statusCode(200)
                .body("customer_name", equalTo(created.getCustomerName()))
                .body("project_name", equalTo(created.getProjectName()))
                .body("project_id", equalTo(1234))
                .body("description", equalTo(created.getDescription()));

        // process updated
        given()
            .when()
                .auth()
                .oauth2(token)
                .contentType(ContentType.JSON)
                .put("/engagements/process/modified")
            .then()
                .statusCode(200);

    }

    @Test
    public void testPutEngagementProcessModifiedWithAuthAndRoleSuccessCreateAndUpdateBeforeProcess() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = mockEngagement();
        engagement.setDescription(SCENARIO.SUCCESS.getValue());

        String body = quarkusJsonb.toJson(engagement);

        // POST engagement - create
        Response response = given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .post("/engagements");

        assertEquals(201, response.getStatusCode());

        String responseBody = response.getBody().asString();
        Engagement created = quarkusJsonb.fromJson(responseBody, Engagement.class);

        assertEquals(engagement.getCustomerName(), created.getCustomerName());
        assertEquals(engagement.getProjectName(), created.getProjectName());
        assertNull(created.getProjectId());
        assertNotNull(created.getLastUpdate());

        // update description
        created.setEngagementLeadName("mr.el");
        body = quarkusJsonb.toJson(created);

        // PUT engagement - update
        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/customers/" + engagement.getCustomerName() + "/projects/" + engagement.getProjectName())
            .then()
                .statusCode(200)
                .body("customer_name", equalTo(created.getCustomerName()))
                .body("project_name", equalTo(created.getProjectName()))
                .body("project_id", nullValue())
                .body("description", equalTo(created.getDescription()))
                .body("engagement_lead_name", equalTo(created.getEngagementLeadName()));

        // process updated
        given()
            .when()
                .auth()
                .oauth2(token)
                .contentType(ContentType.JSON)
                .put("/engagements/process/modified")
            .then()
                .statusCode(200);

    }

    @Test
    public void testPutEngagementProcessModifiedWithAuthAndRoleGitApiException() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = mockEngagement();
        engagement.setDescription(SCENARIO.RUNTIME_EXCEPTION.getValue());

        String body = quarkusJsonb.toJson(engagement);

        // POST engagement - create
        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .post("/engagements")
            .then()
                .statusCode(201)
                .body("customer_name", equalTo(engagement.getCustomerName()))
                .body("project_name", equalTo(engagement.getProjectName()))
                .body("project_id", nullValue());


        // create another engagement
        engagement = mockEngagement();
        engagement.setCustomerName("AnotherTestCustomer");
        engagement.setProjectName("AnotherTestProject");
        engagement.setDescription(SCENARIO.SUCCESS.getValue());
        body = quarkusJsonb.toJson(engagement);

        // POST engagement - create
        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .post("/engagements")
            .then()
                .statusCode(201)
                .body("customer_name", equalTo(engagement.getCustomerName()))
                .body("project_name", equalTo(engagement.getProjectName()))
                .body("project_id", nullValue());

        // sync to git api
        given()
            .when()
                .auth()
                .oauth2(token)
                .contentType(ContentType.JSON)
                .put("/engagements/process/modified")
            .then()
                .statusCode(200);

    }

    private Engagement mockEngagement() {

        Engagement engagement = Engagement.builder().customerName("TestCustomer").projectName("TestProject")
                .description("Test Description").location("Raleigh, NC").startDate("20170501").endDate("20170708")
                .archiveDate("20170930").engagementLeadName("Mister Lead").engagementLeadEmail("mister@lead.com")
                .technicalLeadName("Mister Techlead").technicalLeadEmail("mister@techlead.com")
                .customerContactName("Customer Contact").customerContactEmail("customer@contact.com").build();

        return engagement;

    }

}
