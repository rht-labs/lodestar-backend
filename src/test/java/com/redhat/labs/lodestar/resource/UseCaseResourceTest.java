package com.redhat.labs.lodestar.resource;

import com.redhat.labs.lodestar.model.UseCase;
import com.redhat.labs.lodestar.rest.client.UseCaseApiClient;
import com.redhat.labs.lodestar.utils.TokenUtils;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
@Tag("nested")
public class UseCaseResourceTest {

    static String validToken =  TokenUtils.generateTokenString("/JwtClaimsWriter.json");

    @InjectMock
    @RestClient
    UseCaseApiClient useCaseApiClient;

    @Test
    void testGetUseCase() {

        UseCase useCase = UseCase.builder().title("useCase1").description("use case 1").order(0).build();
        javax.ws.rs.core.Response response = javax.ws.rs.core.Response.ok(List.of(useCase))
                .header("x-total-use-cases", 1).build();

        Mockito.when(useCaseApiClient.getUseCases(0, 500, Collections.emptySet())).thenReturn(response);
        given()
                .auth()
                .oauth2(validToken)
                .contentType(ContentType.JSON)
                .when()
                .get("engagements/usecases")
                .then()
                .statusCode(200)
                .body(containsString("useCase1"))
                .body(containsString("use case 1"));

    }
}
