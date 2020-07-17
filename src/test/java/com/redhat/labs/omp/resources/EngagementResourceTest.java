package com.redhat.labs.omp.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;

import javax.inject.Inject;
import javax.json.bind.Jsonb;

import org.junit.jupiter.api.Test;

import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.omp.rest.client.MockOMPGitLabAPIService.SCENARIO;
import com.redhat.labs.utils.EmbeddedMongoTest;
import com.redhat.labs.utils.TokenUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@EmbeddedMongoTest
@QuarkusTest
public class EngagementResourceTest {

    @Inject
    Jsonb quarkusJsonb;

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
    public void testPostEngagementWithWrongRole() throws Exception {

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
    public void testPostEngagementWithAuthAndRoleSuccess() throws Exception {

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


    }

    @Test
    public void testPostEngagementWithAuthAndRoleInvalidCustomerNameWhitespace() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = mockEngagement();
        engagement.setCustomerName("  ");
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
    public void testPostEngagementWithAuthAndRoleInvalidCustomerNameNull() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = mockEngagement();
        engagement.setCustomerName(null);
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
    public void testPostEngagementWithAuthAndRoleInvalidCustomerNameEmptyString() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = mockEngagement();
        engagement.setCustomerName("");
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
    public void testPostEngagementWithAuthAndRoleInvalidProjectNameWhitespace() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = mockEngagement();
        engagement.setProjectName("  ");
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
    public void testPostEngagementWithAuthAndRoleInvalidProjectNameNull() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = mockEngagement();
        engagement.setProjectName(null);
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
    public void testPostEngagementWithAuthAndRoleInvalidProjectNameEmptyString() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", timeClaims);

        Engagement engagement = mockEngagement();
        engagement.setProjectName("");
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
    public void testPostEngagementWithAuthAndRoleEngagemenntAlreadyExists() throws Exception {

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
    public void testPutEngagementWithAuthAndRoleSuccess() throws Exception {

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
                .body("description", equalTo(created.getDescription()));

    }

    @Test
    public void testPutEngagementWithAuthAndRoleInvalidCustomerNameWhitespace() throws Exception {

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
    public void testPutEngagementWithAuthAndRoleInvalidCustomerNameEmpty() throws Exception {

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
    public void testPutEngagementWithAuthAndRoleInvalidCustomerNameNull() throws Exception {

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
    public void testPutEngagementWithAuthAndRoleInvalidProjectNameWhitespace() throws Exception {

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
    public void testPutEngagementWithAuthAndRoleInvalidProjectNameEmpty() throws Exception {

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
    public void testPutEngagementWithAuthAndRoleInvalidProjectNameNull() throws Exception {

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
    public void testPutEngagementWithAuthAndRoleEngagementDoesNotExist() throws Exception {

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
    public void testGetEngagementWithAuthAndRoleSuccess() throws Exception {

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
                .get("/engagements/customers/" + engagement.getCustomerName() + "/projects/" + engagement.getProjectName())
            .then()
                .statusCode(200)
                .body("customer_name", equalTo(engagement.getCustomerName()))
                .body("project_name", equalTo(engagement.getProjectName()))
                .body("project_id", nullValue());

    }

    @Test
    public void testGetEngagementWithAuthAndRoleDoesNotExist() throws Exception {

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
    public void testGetEngagementWithAuthAndRoleSuccessNoEngagements() throws Exception {

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
    public void testGetEngagementWithAuthAndRoleSuccessEngagments() throws Exception {

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
    public void testLaunchEngagementWithAuthAndRoleSuccess() throws Exception {

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
    public void testLaunchEngagementWithAuthAndRoleEngagementNotFound() throws Exception {

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
    @Test
    public void testPostEngagementWithAuthAndRoleHasUserClaim() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/jwt/user-claims/JwtClaimsAllWithNameClaim.json", timeClaims);

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
                .body("last_update_by_name", equalTo("John Doe"))
                .body("last_update_by_email", equalTo("jdoe@test.com"));


    }

    @Test
    public void testPostEngagementWithAuthAndRoleHasNoUserClaim() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/jwt/user-claims/JwtClaimsAllWithNoNameClaim.json", timeClaims);

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
                .body("last_update_by_name", equalTo("jdoe"))
                .body("last_update_by_email", equalTo("jdoe@test.com"));


    }

    @Test
    public void testPostEngagementWithAuthAndRoleHasEmptyUserClaim() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/jwt/user-claims/JwtClaimsAllWithEmptyNameClaim.json", timeClaims);

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
                .body("last_update_by_name", equalTo("jdoe"))
                .body("last_update_by_email", equalTo("jdoe@test.com"));


    }

    @Test
    public void testPostEngagementWithAuthAndRoleHasBlankUserClaim() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/jwt/user-claims/JwtClaimsAllWithBlankNameClaim.json", timeClaims);

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
                .body("last_update_by_name", equalTo("jdoe"))
                .body("last_update_by_email", equalTo("jdoe@test.com"));


    }

    @Test
    public void testPostEngagementWithAuthAndRoleHasNoUsernameClaim() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/jwt/user-claims/JwtClaimsAllNoNameNoUsername.json", timeClaims);

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
                .body("last_update_by_name", equalTo("jdoe@test.com"))
                .body("last_update_by_email", equalTo("jdoe@test.com"));


    }

    @Test
    public void testPostEngagementWithAuthAndRoleHasEmptyUsernameClaim() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/jwt/user-claims/JwtClaimsAllNoNameEmptyUsername.json", timeClaims);

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
                .body("last_update_by_name", equalTo("jdoe@test.com"))
                .body("last_update_by_email", equalTo("jdoe@test.com"));


    }

    @Test
    public void testPostEngagementWithAuthAndRoleHasBlankUsernameClaim() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/jwt/user-claims/JwtClaimsAllNoNameBlankUsername.json", timeClaims);

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
                .body("last_update_by_name", equalTo("jdoe@test.com"))
                .body("last_update_by_email", equalTo("jdoe@test.com"));


    }

    @Test
    public void testPostEngagementWithAuthAndRoleHasNoEmailClaim() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/jwt/user-claims/JwtClaimsAllNoNameNoUsernameNoEmail.json", timeClaims);

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
                .body("last_update_by_name", equalTo("lodestar-email"))
                .body("last_update_by_email", equalTo("lodestar-email"));


    }

    @Test
    public void testPostEngagementWithAuthAndRoleHasEmptyEmailClaim() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/jwt/user-claims/JwtClaimsAllNoNameNoUsernameEmptyEmail.json", timeClaims);

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
                .body("last_update_by_name", equalTo("lodestar-email"))
                .body("last_update_by_email", equalTo("lodestar-email"));


    }

    @Test
    public void testPostEngagementWithAuthAndRoleHasBlankEmailClaim() throws Exception {

        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/jwt/user-claims/JwtClaimsAllNoNameNoUsernameBlankEmail.json", timeClaims);

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
                .body("last_update_by_name", equalTo("lodestar-email"))
                .body("last_update_by_email", equalTo("lodestar-email"));


    }

    @Test
    public void testHeadEngagementWithAuthAndRoleSuccess() throws Exception {

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
    public void testHeadEngagementWithAuthAndRoleNptFound() throws Exception {

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

    public Engagement mockEngagement() {

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
