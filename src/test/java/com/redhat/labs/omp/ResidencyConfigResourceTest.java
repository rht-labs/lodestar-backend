package com.redhat.labs.omp;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ResidencyConfigResourceTest {

	@Test
	public void getResidencyConfigTest() {
		given().when().get("/residency/config").then().statusCode(200).body("config", is("dyi"));
	}
}
