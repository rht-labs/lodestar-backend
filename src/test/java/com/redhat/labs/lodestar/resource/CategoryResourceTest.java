package com.redhat.labs.lodestar.resource;

import com.redhat.labs.lodestar.rest.client.CategoryApiClient;
import com.redhat.labs.lodestar.utils.TokenUtils;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.util.Set;
import java.util.TreeSet;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@Tag("nested")
public class CategoryResourceTest {

    static String validToken =  TokenUtils.generateTokenString("/JwtClaimsWriter.json");

    @InjectMock
    @RestClient
    CategoryApiClient categoryApiClient;

    @ParameterizedTest
    @MethodSource("nullEmptyBlankSource")
    void testGetCategories(String input) {

        Mockito.when(categoryApiClient.getCategorySuggestions(Mockito.anyString())).thenReturn(new TreeSet<>(Set.of("cat1", "cat2", "zzzz")));

        // get suggestions
        given()
                .auth()
                .oauth2(validToken)
                .queryParam("q", input)
                .contentType(ContentType.JSON)
                .when()
                .get("/engagements/categories/suggest")
                .then()
                .statusCode(200)
                .body("size()", equalTo(3))
                .body("[0]", equalTo("cat1"));
    }

    @Test
    void testGetCategories() {

        Mockito.when(categoryApiClient.getCategorySuggestions(Mockito.anyString())).thenReturn(new TreeSet<>(Set.of("sugar", "suggest", "autosuggest")));

        // get suggestions
        given()
                .auth()
                .oauth2(validToken)
                .queryParam("q", "sug")
                .contentType(ContentType.JSON)
                .when()
                .get("/engagements/categories/suggest")
                .then()
                .statusCode(200)
                .body("size()", equalTo(3))
                .body("[0]", equalTo("autosuggest"));
    }
}
