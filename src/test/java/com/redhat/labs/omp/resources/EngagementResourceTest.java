package com.redhat.labs.omp.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import java.util.HashMap;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.PropertyNamingStrategy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.labs.mocks.MockOMPGitLabAPIService.SCENARIO;
import com.redhat.labs.omp.model.Engagement;
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

@QuarkusTest
public class EngagementResourceTest {

    JsonbConfig config = new JsonbConfig()
            .withPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CASE_WITH_UNDERSCORES);
    Jsonb jsonb = JsonbBuilder.create(config);

    private static final MongodStarter starter = MongodStarter.getDefaultInstance();

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
     *  POST SCENARIOS:
     *    NEGATIVE:
     *    - token has wrong role, returns 403
     *    - engagement already exists with customer/project key
     *    - engagement does not exist, persist, git returns non 201 response, get has engagement no engagement id
     *    - engagement does not exist, persist, git throws exception, get has engagement, no engagement id
     *    POSITIVE:
     *    - engagement does not exist, persist, do get should have engagementId
     *  
     */

    @Test
    public void testPostEngagementWithWrongRole() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsReader.json", timeClaims);

        String body = toJsonString(mockEngagement());

        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .post("/engagements")
            .then()
                .statusCode(403);

    }

    @Test
    public void testPostEngagementWithAuthAndRoleSuccess() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = mockEngagement();
        engagement.setDescription(SCENARIO.SUCCESS.getValue());

        String body = toJsonString(engagement);

        // POST engagement
        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .post("/engagements")
            .then()
                .statusCode(201);

        // Validate async update of engagement id
        given()
            .when()
                .auth()
                .oauth2(token)
                .get("/engagements/customers/" + engagement.getCustomerName() + "/projects/" + engagement.getProjectName())
            .then()
                .statusCode(200)
                .body("engagement_id", nullValue());

    }

    @Test
    public void testPostEngagementWithAuthAndRoleEngagemenntAlreadyExists() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = mockEngagement();
        engagement.setDescription(SCENARIO.SUCCESS.getValue());

        String body = toJsonString(engagement);

        // POST engagement
        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .post("/engagements")
            .then()
                .statusCode(201);

        // POST engagement again
        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .post("/engagements")
            .then()
                .statusCode(409);


    }

    @Test
    public void testPostEngagementWithAuthAndRolePersistedGitApiReturnsNon201() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = mockEngagement();
        engagement.setDescription(SCENARIO.SERVER_ERROR.getValue());

        String body = toJsonString(engagement);

        // POST engagement
        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .post("/engagements")
            .then()
                .statusCode(201);

        // Validate async failed to update of engagement id
        given()
            .when()
                .auth()
                .oauth2(token)
                .get("/engagements/customers/" + engagement.getCustomerName() + "/projects/" + engagement.getProjectName())
            .then()
                .statusCode(200)
                .body("engagement_id", nullValue());


    }

    @Test
    public void testPostEngagementWithAuthAndRolePersistedGitApiReturnsException() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = mockEngagement();
        engagement.setDescription(SCENARIO.RUNTIME_EXCEPTION.getValue());

        String body = toJsonString(engagement);

        // POST engagement
        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .post("/engagements")
            .then()
                .statusCode(201);

        // Validate async failed to update of engagement id
        given()
            .when()
                .auth()
                .oauth2(token)
                .get("/engagements/customers/" + engagement.getCustomerName() + "/projects/" + engagement.getProjectName())
            .then()
                .statusCode(200)
                .body("engagement_id", nullValue());


    }

    /*
     * PUT SCENARIOS:
     *   NEGATIVE:
     *     - engagement does not exist, 404 returned
     *     - engagement exists
     *   POSITIVE:
     *     - engagement exists, is updated in db, git api call successful
     */

    /*
     * GET SCENARIOS:
     *   NEGATIVE:
     *     - Engagement does not exist, 404 returned
     *   POSITIVE:
     *     - Engagement exists and returned
     */

    /*
     * DELETE SCENARIOS:
     *   NEGATIVE:
     *   POSITIVE:
     *     - No engagements exists in db, empty list returned
     *     - Engagements exists, array containing engagements returned
     */

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

    private String toJsonString(Engagement engagement) {
        return jsonb.toJson(engagement);
    }

}
