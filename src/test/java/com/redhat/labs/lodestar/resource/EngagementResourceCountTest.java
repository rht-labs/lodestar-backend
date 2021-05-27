package com.redhat.labs.lodestar.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.Launch;
import com.redhat.labs.lodestar.service.EngagementService;
import com.redhat.labs.lodestar.utils.IntegrationTestHelper;
import com.redhat.labs.lodestar.utils.MockUtils;
import com.redhat.labs.lodestar.utils.TokenUtils;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@Tag("nested")
public class EngagementResourceCountTest extends IntegrationTestHelper {

    @Inject
    EngagementService service;

    @Test
    public void testest() throws Exception {

        String url = "/engagements/count";
        String token = TokenUtils.generateTokenString("/JwtClaimsWriter.json", new HashMap<String, Long>());

        List<Engagement> all = new ArrayList<>();

        all.add(MockUtils.mockEngagement());
        all.add(MockUtils.mockEngagement());

        Mockito.when(eRepository.listAll()).thenReturn(all);

        given().when().auth().oauth2(token).get(url).then().statusCode(200).body("UPCOMING", equalTo(2));
        
        for(Engagement e : all) {
            e.setLaunch(new Launch());
        }
        
        given().when().auth().oauth2(token).queryParam("localTime", "2021-06-08T00:00:00.000Z").get(url).then().statusCode(200).body("PAST", equalTo(2));
    }

}
