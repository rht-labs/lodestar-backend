package com.redhat.labs.omp.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;

import javax.inject.Inject;
import javax.json.bind.Jsonb;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.omp.rest.client.MockOMPGitLabAPIService.SCENARIO;
import com.redhat.labs.utils.TokenUtils;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@QuarkusTest
public class GitSyncResourceTest {

    private static final MongodStarter starter = MongodStarter.getDefaultInstance();

    @Inject
    Jsonb quarkusJsonb;

    private MongodExecutable _mongodExe;
    private MongodProcess _mongod;

    @BeforeEach
    protected void setUp() throws Exception {

        // setup local config
        IMongodConfig config = new MongodConfigBuilder()
                .version(Version.Main.PRODUCTION)
                .net(new Net("localhost", 12345, Network.localhostIsIPv6()))
                .build();

        // create executable
        _mongodExe = starter.prepare(config);
        // start mongo
        _mongod = _mongodExe.start();

    }

    @AfterEach
    protected void tearDown() throws Exception {

        _mongod.stop();
        _mongodExe.stop();

    }

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
                .body("engagement_id", nullValue());


        // process created
        given()
            .when()
                .auth()
                .oauth2(token)
                .contentType(ContentType.JSON)
                .put("/engagements/process/modified")
            .then()
                .statusCode(200);

        // update description
        engagement.setDescription("updated");
        body = quarkusJsonb.toJson(engagement);

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
                .body("customer_name", equalTo(engagement.getCustomerName()))
                .body("project_name", equalTo(engagement.getProjectName()))
                .body("engagement_id", equalTo(1234))
                .body("description", equalTo(engagement.getDescription()));

        // process updated
        given()
            .when()
                .auth()
                .oauth2(token)
                .contentType(ContentType.JSON)
                .put("/engagements/process/modified")
            .then()
                .statusCode(200);

        // DELETE engagement - delete
        given()
            .when()
                .auth()
                .oauth2(token)
                .delete("/engagements/customers/" + engagement.getCustomerName() + "/projects/" + engagement.getProjectName())
            .then()
                .statusCode(204);

        // process delete
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
                .body("engagement_id", nullValue());


        // update description
        engagement.setEngagementLeadName("mr.el");
        body = quarkusJsonb.toJson(engagement);

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
                .body("customer_name", equalTo(engagement.getCustomerName()))
                .body("project_name", equalTo(engagement.getProjectName()))
                .body("engagement_id", nullValue())
                .body("description", equalTo(engagement.getDescription()))
                .body("engagement_lead_name", equalTo(engagement.getEngagementLeadName()));

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

    /*
     * GIT 2 Backend Refresh
     *  - create 1, then run sync
     *  - git api returns 1 or 2 engagements, updates mongo 
     *  - do get all (should match what is returned from git api)
     */
    @Test
    public void testRefreshFromGit() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = mockEngagement();
        engagement.setDescription(SCENARIO.SUCCESS.getValue());

        String body = quarkusJsonb.toJson(engagement);

        // POST engagement
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
                .body("engagement_id", nullValue());

        // GET
        given()
            .when()
                .auth()
                .oauth2(token)
                .get("/engagements/customers/" + engagement.getCustomerName() + "/projects/" + engagement.getProjectName())
            .then()
                .statusCode(200)
                .body("customer_name", equalTo(engagement.getCustomerName()))
                .body("project_name", equalTo(engagement.getProjectName()))
                .body("engagement_id", nullValue());

        // Run sync
        given()
            .when()
            .auth()
            .oauth2(token)
            .put("/engagements/refresh")
        .then()
            .statusCode(200);

        // GET all engagement
        Response response = 
        given()
            .when()
                .auth()
                .oauth2(token)
                .contentType(ContentType.JSON)
                .get("/engagements");

        assertEquals(200, response.getStatusCode());
        Engagement[] engagements = quarkusJsonb.fromJson(response.getBody().asString(), Engagement[].class);
        assertEquals(1, engagements.length);

        assertEquals(4321, engagements[0].getEngagementId());
        assertEquals("anotherCustomer", engagements[0].getCustomerName());
        assertEquals("anotherProject", engagements[0].getProjectName());

    }

    private Engagement mockEngagement() {

        Engagement engagement = Engagement.builder().customerName("TestCustomer").projectName("TestProject")
                .description("Test Description").location("Raleigh, NC").startDate("20170501").endDate("20170708")
                .archiveDate("20170930").engagementLeadName("Mister Lead").engagementLeadEmail("mister@lead.com")
                .technicalLeadName("Mister Techlead").technicalLeadEmail("mister@techlead.com")
                .customerContactName("Customer Contact").customerContactEmail("customer@contact.com")
                .ocpCloudProviderName("GCP").ocpCloudProviderRegion("West").ocpVersion("v4.2").ocpSubDomain("jello")
                .ocpPersistentStorageSize("50GB").ocpClusterSize("medium").build();

        return engagement;

    }

}
