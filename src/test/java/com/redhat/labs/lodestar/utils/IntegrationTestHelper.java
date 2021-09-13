package com.redhat.labs.lodestar.utils;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.PropertyNamingStrategy;

import com.redhat.labs.lodestar.rest.client.*;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.test.junit.mockito.InjectMock;

public class IntegrationTestHelper {

    JsonbConfig config = new JsonbConfig().withFormatting(true)
            .withPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CASE_WITH_UNDERSCORES);
    public Jsonb quarkusJsonb = JsonbBuilder.create(config);

    @InjectMock
    @RestClient
    public ConfigApiClient configApiClient;
    
    @InjectMock
    @RestClient
    public ArtifactApiClient artifactClient;

    @InjectMock
    @RestClient
    public EngagementApiClient engagementApiClient;

    public static String[] nullEmptyBlankSource() {
        return new String[] { null, "", "   " };
    }

}