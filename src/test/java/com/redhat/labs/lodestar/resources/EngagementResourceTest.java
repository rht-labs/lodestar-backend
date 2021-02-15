package com.redhat.labs.lodestar.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.json.bind.Jsonb;

import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import com.redhat.labs.lodestar.model.Artifact;
import com.redhat.labs.lodestar.model.Category;
import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.HostingEnvironment;
import com.redhat.labs.lodestar.rest.client.MockLodeStarGitLabAPIService.SCENARIO;
import com.redhat.labs.lodestar.utils.EmbeddedMongoTest;
import com.redhat.labs.lodestar.utils.TokenUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@EmbeddedMongoTest
@QuarkusTest
class EngagementResourceTest {

    @Inject
    Jsonb quarkusJsonb;

    private static String[] nullEmptyBlankSource() {
        return new String[] {null, "", "   "};
    }
    
    /*
     * POST SCENARIOS:
     * Positive:
     *  - post engagement with project/customer name, get returns engagement
     * Negative:
     *  - post engagement, invalid project name (whitespace, empty string, null)
     *  - post engagement, invalid customer name (whitespace, empty string, null)
     *  - post engagement, engagement already exists with customer/project key
     *  
     */

    @Test
    void testPostEngagementWithWrongRole() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsReader.json", timeClaims);

        String body = quarkusJsonb.toJson(mockEngagement());

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
    void testPostEngagementWithAuthAndRoleSuccess() throws Exception {

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
                .body("public_reference", equalTo(engagement.getPublicReference()))
                .body("project_id", nullValue());


    }

    @ParameterizedTest
    @MethodSource("nullEmptyBlankSource")
    void testPostEngagementWithAuthAndRoleInvalidCustomerName(String input) throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = mockEngagement();
        engagement.setCustomerName(input);
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
                .statusCode(400);

    }

    @ParameterizedTest
    @MethodSource("nullEmptyBlankSource")
    void testPostEngagementWithAuthAndRoleInvalidProjectName(String input) throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = mockEngagement();
        engagement.setProjectName(input);
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
                .statusCode(400);

    }

    @Test
    void testPostEngagementWithAuthAndRoleEngagemenntAlreadyExists() throws Exception {

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

    /*
     * PUT SCENARIOS:
     * Postive:
     *  - put engagement, engagement exists, updated in mongo, returns updated
     * Negative:
     *  - put engagement, invalid customer name (whitespace, empty string, null)
     *  - put engagement, invalid project name (whitespace, empty string, null)
     *  - put engagement, engagement does not exist
     */

    @Test
    void testPutEngagementWithAuthAndRoleSuccess() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = mockEngagement();
        engagement.setDescription(SCENARIO.SUCCESS.getValue());

        String body = quarkusJsonb.toJson(engagement);

        // POST engagement
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
        created.setDescription("updated");

        body = quarkusJsonb.toJson(created);

        // delay for async process
        TimeUnit.SECONDS.sleep(1);

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

    }

    @Test
    void testPutEngagementWithAuthAndRoleInvalidCustomerNameWhitespace() throws Exception {

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
                .body("project_id", nullValue());

        // update description
        engagement.setDescription("updated");
        engagement.setCustomerName("  ");
        body = quarkusJsonb.toJson(engagement);

        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/customers/" + engagement.getCustomerName() + "/projects/" + engagement.getProjectName())
            .then()
                .statusCode(400);

    }

    @Test
    void testPutEngagementWithAuthAndRoleInvalidCustomerNameEmpty() throws Exception {

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
                .body("project_id", nullValue());

        // update description
        engagement.setDescription("updated");
        engagement.setCustomerName("");
        body = quarkusJsonb.toJson(engagement);

        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/customers/TestCustomer/projects/" + engagement.getProjectName())
            .then()
                .statusCode(400);

    }

    @Test
    void testPutEngagementWithAuthAndRoleInvalidCustomerNameNull() throws Exception {

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
                .body("project_id", nullValue());

        // update description
        engagement.setDescription("updated");
        engagement.setCustomerName(null);
        body = quarkusJsonb.toJson(engagement);

        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/customers/" + engagement.getCustomerName() + "/projects/" + engagement.getProjectName())
            .then()
                .statusCode(400);

    }

    @Test
    void testPutEngagementWithAuthAndRoleInvalidProjectNameWhitespace() throws Exception {

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
                .body("project_id", nullValue());

        // update description
        engagement.setDescription("updated");
        engagement.setProjectName("  ");
        body = quarkusJsonb.toJson(engagement);

        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/customers/" + engagement.getCustomerName() + "/projects/TestProject")
            .then()
                .statusCode(400);

    }

    @Test
    void testReturnOkIfSubdomainExists() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        String subdomain = "asuperrandomsubdomain";

        given().when().auth().oauth2(token).head(String.format("/engagements/subdomain/%s", subdomain)).then()
                .statusCode(200);

    }

    @Test
    void testReturnConflictIfSubdomainExists() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = mockEngagement();
        String subdomain = "asuperrandomsubdomain";
        engagement.setProjectName("aRandomProjectName");
        HostingEnvironment env = HostingEnvironment.builder().environmentName("e1").ocpSubDomain(subdomain).build();
        engagement.setHostingEnvironments(Arrays.asList(env));

        String body = quarkusJsonb.toJson(engagement);

        given().when().auth().oauth2(token).body(body).contentType(ContentType.JSON).post("/engagements").then()
                .statusCode(201);
        given().when().auth().oauth2(token).head(String.format("/engagements/subdomain/%s", subdomain)).then()
                .statusCode(409);

    }

    @Test
    void testEngagementWithSubdomainAlreadyExists() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = mockEngagement();
        engagement.setProjectName("aRandomProjectName");
        HostingEnvironment env = HostingEnvironment.builder().environmentName("e1").ocpSubDomain("aSuperRandomSubdomain").build();
        engagement.setHostingEnvironments(Arrays.asList(env));

        Engagement engagement2 = mockEngagement();
        engagement2.setProjectName("anotherRandomName");
        HostingEnvironment env2 = HostingEnvironment.builder().environmentName("e2").ocpSubDomain("aSuperRandomSubdomain").build();
        engagement2.setHostingEnvironments(Arrays.asList(env2));

        String body = quarkusJsonb.toJson(engagement);
        String body2 = quarkusJsonb.toJson(engagement2);

        given().when().auth().oauth2(token).body(body).contentType(ContentType.JSON).post("/engagements").then()
                .statusCode(201);
        given().when().auth().oauth2(token).body(body2).contentType(ContentType.JSON).post("/engagements").then()
                .statusCode(409);

    }

    @Test
    void testPutEngagementWithAuthAndRoleInvalidProjectNameEmpty() throws Exception {

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
                .body("project_id", nullValue());

        // update description
        engagement.setDescription("updated");
        engagement.setProjectName("");
        body = quarkusJsonb.toJson(engagement);

        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/customers/" + engagement.getCustomerName() + "/projects/TestProject")
            .then()
                .statusCode(400);

    }

    @Test
    void testPutEngagementWithAuthAndRoleInvalidProjectNameNull() throws Exception {

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
                .body("project_id", nullValue());

        // update description
        engagement.setDescription("updated");
        engagement.setProjectName(null);
        body = quarkusJsonb.toJson(engagement);

        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/customers/" + engagement.getCustomerName() + "/projects/" + engagement.getProjectName())
            .then()
                .statusCode(400);

    }

    @Test
    void testPutEngagementWithAuthAndRoleDuplicateUsers() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = mockEngagement();
        engagement.setDescription(SCENARIO.SUCCESS.getValue());

        String body = quarkusJsonb.toJson(engagement);

        // POST engagement
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
        created.setDescription("updated");

        body = quarkusJsonb.toJson(created);

        // add users with duplicates
        body = addDupliateUsers(body);

        // delay for async process
        TimeUnit.SECONDS.sleep(1);

        Response r =
        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/customers/" + engagement.getCustomerName() + "/projects/" + engagement.getProjectName());

        assertEquals(200, r.getStatusCode());
        String responseJson = r.getBody().asString();
        Engagement updatedEngagement = quarkusJsonb.fromJson(responseJson, Engagement.class);
        
        assertEquals(created.getCustomerName(), updatedEngagement.getCustomerName());
        assertEquals(created.getProjectName(), updatedEngagement.getProjectName());

        // validate users
        assertEquals(2, updatedEngagement.getEngagementUsers().size());

    }

    @Test
    void testPutEngagementWithAuthAndRoleEngagementDoesNotExist() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = mockEngagement();

        // update description
        engagement.setDescription("updated");
        String body = quarkusJsonb.toJson(engagement);

        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/customers/" + engagement.getCustomerName() + "/projects/" + engagement.getProjectName())
            .then()
                .statusCode(404);

    }
    /*
     * GET BY CUSTOMER AND PROJECT SCENARIOS:
     * Postive:
     *  - get, engagement exists, returns
     * Negative:
     *  - get engagement doesn't exist, 404
     */

    @Test
    void testGetEngagementWithAuthAndRoleSuccess() throws Exception {

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
                .body("project_id", nullValue());

        // delay for async process
        TimeUnit.SECONDS.sleep(1);

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
                .body("project_id", equalTo(1234));

    }

    @Test
    void testGetEngagementWithAuthAndRoleDoesNotExist() throws Exception {

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
                .body("project_id", nullValue());

        // GET
        given()
            .when()
                .auth()
                .oauth2(token)
                .get("/engagements/customers/" + engagement.getCustomerName() + "/projects/project2)")
            .then()
                .statusCode(404);

    }

    /*
     *  GET ALL SCENARIOS:
     *  Positive:
     *   - get, no engagements, empty List
     *   - get, engagements, List
     */
    @Test
    void testGetEngagementWithAuthAndRoleSuccessNoEngagements() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        // GET engagement
        Response response = 
        given()
            .when()
                .auth()
                .oauth2(token)
                .contentType(ContentType.JSON)
                .get("/engagements");

        assertEquals(200, response.getStatusCode());
        Engagement[] engagements = response.getBody().as(Engagement[].class);
        assertEquals(0, engagements.length);

    }

    @Test
    void testGetEngagementWithAuthAndRoleSuccessEngagments() throws Exception {

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
                .body("project_id", nullValue());

        // GET engagement
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

    }

    /*
     * Launch Tests
     * Positive:
     *  - engagement exists, call launch, launch data on returned engagement
     * Negagive:
     *  - engagement does not exist, 404 returned
     */

    @Test
    void testLaunchEngagementWithAuthAndRoleSuccess() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = mockEngagement();
        engagement.setDescription(SCENARIO.SUCCESS.getValue());

        String body = quarkusJsonb.toJson(engagement);

        // POST engagement
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

        body = quarkusJsonb.toJson(created);

        // wait for project id to be set
        TimeUnit.SECONDS.sleep(1);

        // Launch engagement
        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/launch")
             .then()
                 .statusCode(200)
                 .body("launch.launched_date_time", notNullValue())
                 .body("launch.launched_by", equalTo("John Doe"));

    }

    @Test
    void testLaunchEngagementWithAuthAndRoleEngagementNotFound() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = mockEngagement();
        engagement.setDescription(SCENARIO.SUCCESS.getValue());

        String body = quarkusJsonb.toJson(engagement);

        // Launch engagement
        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/launch")
             .then()
                 .statusCode(404);

    }

    /*
     * Token User Claim Tests
     * - has user claim
     * 
     * - no user claim, has preferred_username
     * - empty string user claim, has preferred_username
     * - blank user claim, has preferred_username
     * 
     * - no user claim, no preferred_username, email
     * - no user claim, empty preferred_username, email
     * - no user claim, blank preferred_username, email
     * 
     * - no user claim, no preferred_username, empty email
     * - no user claim, no preferred_username, blank email
     * - no user claim, no preferred_username, no email
     * 
     */

    @ParameterizedTest
    @CsvSource({
        "/jwt/user-claims/JwtClaimsAllWithNameClaim.json,John Doe,jdoe@test.com",
        "/jwt/user-claims/JwtClaimsAllWithNoNameClaim.json,jdoe,jdoe@test.com",
        "/jwt/user-claims/JwtClaimsAllWithEmptyNameClaim.json,jdoe,jdoe@test.com",
        "/jwt/user-claims/JwtClaimsAllWithBlankNameClaim.json,jdoe,jdoe@test.com",
        "/jwt/user-claims/JwtClaimsAllNoNameNoUsername.json,jdoe@test.com,jdoe@test.com",
        "/jwt/user-claims/JwtClaimsAllNoNameEmptyUsername.json,jdoe@test.com,jdoe@test.com",
        "/jwt/user-claims/JwtClaimsAllNoNameBlankUsername.json,jdoe@test.com,jdoe@test.com",
        "/jwt/user-claims/JwtClaimsAllNoNameNoUsernameNoEmail.json,lodestar-email,lodestar-email",
        "/jwt/user-claims/JwtClaimsAllNoNameNoUsernameEmptyEmail.json,lodestar-email,lodestar-email",
        "/jwt/user-claims/JwtClaimsAllNoNameNoUsernameBlankEmail.json,lodestar-email,lodestar-email"
    })
    void testPostEngagementWithAuthAndRoleHasUserClaim(String claimFile, String lastUpdateName, String lastUpdateEmail) throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString(claimFile, timeClaims);

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
                .body("project_id", nullValue())
                .body("last_update_by_name", equalTo(lastUpdateName))
                .body("last_update_by_email", equalTo(lastUpdateEmail));


    }

    @Test
    void testHeadEngagementWithAuthAndRoleSuccess() throws Exception {

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
                .body("project_id", nullValue());

        // HEAD
        given()
            .when()
                .auth()
                .oauth2(token)
                .head("/engagements/customers/" + engagement.getCustomerName() + "/projects/" + engagement.getProjectName())
            .then()
                .statusCode(200)
                .header("last-update", notNullValue())
                .header("Access-Control-Expose-Headers", "last-update");

    }

    @Test
    void testHeadEngagementWithAuthAndRoleNptFound() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = mockEngagement();

        // HEAD
        given()
            .when()
                .auth()
                .oauth2(token)
                .head("/engagements/customers/" + engagement.getCustomerName() + "/projects/" + engagement.getProjectName())
            .then()
                .statusCode(404);

    }

    /*
     * GIT 2 Backend Refresh
     *  - create 1, then run sync
     *  - git api returns 1 or 2 engagements, updates mongo 
     *  - do get all (should match what is returned from git api)
     */
    @Test
    void testRefreshFromGit() throws Exception {

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
                .body("project_id", nullValue());

        // delay for async process
        TimeUnit.SECONDS.sleep(1);

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
                .body("project_id", equalTo(1234));

        // Run sync
        given()
            .when()
            .auth()
            .oauth2(token)
            .put("/engagements/refresh")
        .then()
            .statusCode(202);

        // make sure the async processes finish
        TimeUnit.SECONDS.sleep(1);

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
        assertEquals(2, engagements.length);

        for(Engagement e : engagements) {

            if(e.getCustomerName().equals("TestCustomer")) {

                assertEquals("TestCustomer", e.getCustomerName());
                assertEquals("TestProject", e.getProjectName());


            } else if(e.getCustomerName().equals("anotherCustomer")) {

                assertEquals(4321, e.getProjectId());
                assertEquals("anotherCustomer", e.getCustomerName());
                assertEquals("anotherProject", e.getProjectName());

            } else {
                fail("unknown customer found in response. " + e.getCustomerName());
            }

        }

    }

    @Test
    void testRefreshFromGitWithPurgeFirst() throws Exception {

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
                .body("project_id", nullValue());

        // delay for async process
        TimeUnit.SECONDS.sleep(1);

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
                .body("project_id", equalTo(1234));

        // Run sync
        given()
            .when()
            .auth()
            .oauth2(token)
            .queryParam("purgeFirst", true)
            .put("/engagements/refresh")
        .then()
            .statusCode(202);

        // make sure the async processes finish
        TimeUnit.SECONDS.sleep(1);

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

        Engagement e = engagements[0];

        assertEquals(4321, e.getProjectId());
        assertEquals("anotherCustomer", e.getCustomerName());
        assertEquals("anotherProject", e.getProjectName());

    }

    @Test
    void testGetAllCategoriesAndGetSuggestion() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        // create engagements with categories
        mockEngagementsWithCategories().stream()
            .forEach(e -> {

                String body = quarkusJsonb.toJson(e);

                given()
                .when()
                    .auth()
                    .oauth2(token)
                    .body(body)
                    .contentType(ContentType.JSON)
                    .post("/engagements")
                .then()
                    .statusCode(201);
                    

            });


        // get all
        Response r =
        given()
            .auth()
            .oauth2(token)
            .contentType(ContentType.JSON)
        .when()
            .get("/engagements/categories");

        assertEquals(200, r.getStatusCode());
        Category[] results = r.as(Category[].class);
        assertEquals(4, results.length);
        Map<String, Boolean> resultsMap = validateCategories(results);

        assertTrue(resultsMap.containsKey("c1") && 
                resultsMap.containsKey("c2") && 
                resultsMap.containsKey("c4") && 
                resultsMap.containsKey("e5"));

        // get suggestions
        r =given()
            .auth()
            .oauth2(token)
            .queryParam("suggest", "c")
            .contentType(ContentType.JSON)
        .when()
            .get("/engagements/categories");

        assertEquals(200, r.getStatusCode());
        results = r.as(Category[].class);
        assertEquals(3, results.length);
        resultsMap = validateCategories(results);

        assertTrue(resultsMap.containsKey("c1") && 
                resultsMap.containsKey("c2") && 
                resultsMap.containsKey("c4") && 
                !resultsMap.containsKey("e5"));

        r = given()
            .auth()
            .oauth2(token)
            .queryParam("suggest", "e")
            .contentType(ContentType.JSON)
        .when()
            .get("/engagements/categories");

        assertEquals(200, r.getStatusCode());
        results = r.as(Category[].class);
        assertEquals(1, results.length);
        resultsMap = validateCategories(results);

        assertTrue(!resultsMap.containsKey("c1") && 
                !resultsMap.containsKey("c2") && 
                !resultsMap.containsKey("c4") && 
                resultsMap.containsKey("e5"));

    }

    @Test
    void testGetArtifactTypes() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        // create engagements with artifacts
        mockEngagementWithArtifacts().stream()
            .forEach(e -> {

                String body = quarkusJsonb.toJson(e);

                given()
                .when()
                    .auth()
                    .oauth2(token)
                    .body(body)
                    .contentType(ContentType.JSON)
                    .post("/engagements")
                .then()
                    .statusCode(201);


            });

        // get all artifact types
        given()
            .auth()
            .oauth2(token)
            .contentType(ContentType.JSON)
        .when()
            .get("/engagements/artifact/types")
        .then()
            .statusCode(200)
            .body(containsString("demo"))
            .body(containsString("report"))
            .body(containsString("note"));

        // get all artifact types by suggestion
        given()
            .auth()
            .oauth2(token)
            .contentType(ContentType.JSON)
            .queryParam("suggest", "de")
        .when()
            .get("/engagements/artifact/types")
        .then()
            .statusCode(200)
            .body(containsString("demo"));

        given()
            .auth()
            .oauth2(token)
            .contentType(ContentType.JSON)
            .queryParam("suggest", "rE")
        .when()
            .get("/engagements/artifact/types")
        .then()
            .statusCode(200)
            .body(containsString("report"));

        given()
            .auth()
            .oauth2(token)
            .contentType(ContentType.JSON)
            .queryParam("suggest", "E")
        .when()
            .get("/engagements/artifact/types")
        .then()
            .statusCode(200)
            .body(containsString("demo"))
            .body(containsString("report"))
            .body(containsString("note"));

    }

    @Test
    void testPostEngagementWithConflictingHostingEvironmentSubdomain() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        // create engagement with hosting environment
        Engagement e1 = mockMinimumEngagement("c1", "p1");
        HostingEnvironment he1 = mockHostingEnvironment("env1", "p1");
        e1.setHostingEnvironments(Arrays.asList(he1));

        String body = quarkusJsonb.toJson(e1);

        // POST first engagement
        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .post("/engagements")
            .then()
                .statusCode(201);

        // create another engagement with hosting environment same as prior engagement
        Engagement e2 = mockMinimumEngagement("c2", "anotherProject");
        HostingEnvironment he2 = mockHostingEnvironment("unique", "p1");
        e2.setHostingEnvironments(Arrays.asList(he2));

        body = quarkusJsonb.toJson(e2);

        // POST first engagement
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
    void testPutEngagementWithExistingHostingEvironmentSubdomain() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        // create engagement with hosting environment
        Engagement e1 = mockMinimumEngagement("c1", "p1");
        HostingEnvironment he1 = mockHostingEnvironment("env1", "p1");
        e1.setHostingEnvironments(Arrays.asList(he1));
        e1.setDescription(SCENARIO.SUCCESS.getValue());

        String body = quarkusJsonb.toJson(e1);

        // POST first engagement
        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .post("/engagements")
            .then()
                .statusCode(201);

        // create another engagement
        Engagement e2 = mockMinimumEngagement("c2", "anotherProject");

        // create second engagement with conflicting hosting environment subdomain
        HostingEnvironment he2 = mockHostingEnvironment("unique", "p2");
        e2.setHostingEnvironments(Arrays.asList(he2));
        e2.setDescription(SCENARIO.SUCCESS.getValue());

        body = quarkusJsonb.toJson(e2);

        // POST second engagement
        Response response = given() 
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .post("/engagements");

        assertEquals(201, response.statusCode());

        String responseBody = response.asString();
        e2 = quarkusJsonb.fromJson(responseBody, Engagement.class);

        body = quarkusJsonb.toJson(e2);

        // delay for async process
        TimeUnit.SECONDS.sleep(1);

        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/" + e2.getUuid())
            .then()
                .statusCode(200);

    }

    @Test
    void testPutEngagementWithConflictingHostingEvironmentSubdomain() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        // create engagement with hosting environment
        Engagement e1 = mockMinimumEngagement("c1", "p1");
        HostingEnvironment he1 = mockHostingEnvironment("env1", "p1");
        e1.setHostingEnvironments(Arrays.asList(he1));
        e1.setDescription(SCENARIO.SUCCESS.getValue());

        String body = quarkusJsonb.toJson(e1);

        // POST first engagement
        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .post("/engagements")
            .then()
                .statusCode(201);

        // create another engagement
        Engagement e2 = mockMinimumEngagement("c2", "anotherProject");
        e2.setDescription(SCENARIO.SUCCESS.getValue());

        body = quarkusJsonb.toJson(e2);

        // POST second engagement
        Response response = given() 
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .post("/engagements");

        assertEquals(201, response.statusCode());

        String responseBody = response.asString();
        e2 = quarkusJsonb.fromJson(responseBody, Engagement.class);

        // update second engagement with conflicting hosting environment subdomain
        HostingEnvironment he2 = mockHostingEnvironment("unique", "p1");
        e2.setHostingEnvironments(Arrays.asList(he2));

        body = quarkusJsonb.toJson(e2);

        // delay for async process
        TimeUnit.SECONDS.sleep(1);

        given()
            .when()
                .auth()
                .oauth2(token)
                .body(body)
                .contentType(ContentType.JSON)
                .put("/engagements/" + e2.getUuid())
            .then()
                .statusCode(409);

    }

    @Test
    void testDeleteEngagement() throws Exception {
        
        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = mockEngagement();
        engagement.setDescription(SCENARIO.SUCCESS.getValue());

        String body = quarkusJsonb.toJson(engagement);

        // POST engagement
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

        assertNotNull(created.getUuid());

        // validate create
        given()
        .when()
            .auth()
            .oauth2(token)
            .get("/engagements/" + created.getUuid())
        .then()
            .statusCode(200);

        // DELETE
        given()
        .when()
            .auth()
            .oauth2(token)
            .delete("/engagements/" + created.getUuid())
        .then()
            .statusCode(202);

        // Validate delete
        given()
        .when()
            .auth()
            .oauth2(token)
            .get("/engagements/" + created.getUuid())
        .then()
            .statusCode(404);

        
    }

    private Map<String, Boolean> validateCategories(Category[] categories) {

        Map<String, Boolean> map = new HashMap<>();

        for(Category c : categories) {

            if("c1".equals(c.getName()) && 1 == c.getCount()) {
                map.put("c1", Boolean.TRUE);
            }

            if("c2".equals(c.getName()) && 2 == c.getCount()) {
                map.put("c2", Boolean.TRUE);
            }

            if("c4".equals(c.getName()) && 1 == c.getCount()) {
                map.put("c4", Boolean.TRUE);
            }

            if("e5".equals(c.getName()) && 1 == c.getCount()) {
                map.put("e5", Boolean.TRUE);
            }

        }

        return map;

    }

    private List<Engagement> mockEngagementWithArtifacts() {

        Artifact a1 = mockArtifact("E1 Week 1 Report", "report", "http://report-week-1");
        Artifact a2 = mockArtifact("E1 Demo Week 1", "demo", "http://demo-week-1");
        Artifact a3 = mockArtifact("E1 Demo Week 2", "demo", "http://demo-week-2");

        Engagement e1 = mockEngagement();
        e1.setCustomerName("customer1");
        e1.setArtifacts(Arrays.asList(a1, a2, a3));

        Artifact a4 = mockArtifact("E2 Week 1 Report", "report", "http://report-week-1");
        Artifact a5 = mockArtifact("E2 Demo Week 1", "demo", "http://demo-week-1");
        Artifact a6 = mockArtifact("E2 Demo Week 2", "demo", "http://demo-week-2");
        Artifact a7 = mockArtifact("E2 Notes", "note", "http://notes");

        Engagement e2 = mockEngagement();
        e2.setCustomerName("customer2");
        e2.setArtifacts(Arrays.asList(a4, a5, a6, a7));

        return Arrays.asList(e1, e2);
    }

    private Artifact mockArtifact(String title, String type, String link) {
        return Artifact.builder().title(title).type(type).linkAddress(link).build();
    }

    private List<Engagement> mockEngagementsWithCategories() {

        Category c1 = mockCategory("c1");
        Category c2 = mockCategory("c2");

        Engagement e1 = mockEngagement();
        e1.setCustomerName("customer1");
        e1.setCategories(Arrays.asList(c1, c2));

        Category c3 = mockCategory("C2");
        Category c4 = mockCategory("c4");
        Category c5 = mockCategory("e5");

        Engagement e2 = mockEngagement();
        e2.setCustomerName("customer2");
        e2.setCategories(Arrays.asList(c3,c4,c5));

        return Arrays.asList(e1, e2);

    }

    private Category mockCategory(String name) {
        return Category.builder().name(name).build();
    }

    public Engagement mockEngagement() {

        Engagement engagement = Engagement.builder().customerName("TestCustomer").projectName("TestProject")
                .description("Test Description").location("Raleigh, NC").startDate("20170501").endDate("20170708")
                .archiveDate("20170930").engagementLeadName("Mister Lead").engagementLeadEmail("mister@lead.com")
                .technicalLeadName("Mister Techlead").technicalLeadEmail("mister@techlead.com")
                .customerContactName("Customer Contact").customerContactEmail("customer@contact.com").build();

        return engagement;

    }

    public Engagement mockMinimumEngagement(String customerName, String projectName) {
        return Engagement.builder().customerName(customerName).projectName(projectName).build();
    }

    HostingEnvironment mockHostingEnvironment(String environmentName, String ocpSubdomain) {
        return HostingEnvironment.builder().environmentName(environmentName).ocpCloudProviderName("provider1")
                .ocpClusterSize("small").ocpPersistentStorageSize("none").ocpSubDomain(ocpSubdomain).ocpVersion("4.x.x")
                .build();
    }

    @SuppressWarnings("unchecked")
    String addDupliateUsers(String engagementJson) throws ParseException {

        JSONParser jsonParser = new JSONParser();
        JSONObject json = (JSONObject) jsonParser.parse(engagementJson);

        Collection<JSONObject> users = new ArrayList<JSONObject>();

        JSONObject user1 = createUserJson("bob", "smith", "developer", "bsmith@example.com");
        JSONObject user2 = createUserJson("john", "jones", "developer", "jjones@example.com");
        JSONObject user3 = createUserJson("bob", "smith", "admin", "bsmith@example.com");

        users.add(user1);
        users.add(user2);
        users.add(user3);

        json.put("engagement_users", users);

        return json.toJSONString();

    }

    @SuppressWarnings("unchecked")
    JSONObject createUserJson(String firstName, String lastName, String role, String email) {

        JSONObject user = new JSONObject();
        user.put("first_name", firstName);
        user.put("last_name", lastName);
        user.put("role", role);
        user.put("email", email);

        return user;

    }

}
