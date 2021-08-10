package com.redhat.labs.lodestar.utils;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.PropertyNamingStrategy;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.redhat.labs.lodestar.repository.ActiveSyncRepository;
import com.redhat.labs.lodestar.repository.EngagementRepository;
import com.redhat.labs.lodestar.rest.client.ArtifactApiClient;
import com.redhat.labs.lodestar.rest.client.ActivityApiClient;
import com.redhat.labs.lodestar.rest.client.ConfigApiClient;
import com.redhat.labs.lodestar.rest.client.LodeStarGitApiClient;
import com.redhat.labs.lodestar.rest.client.LodeStarStatusApiClient;
import com.redhat.labs.lodestar.rest.client.ParticipantApiClient;

import io.quarkus.test.junit.mockito.InjectMock;

public class IntegrationTestHelper {

    JsonbConfig config = new JsonbConfig().withFormatting(true)
            .withPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CASE_WITH_UNDERSCORES);
    public Jsonb quarkusJsonb = JsonbBuilder.create(config);

    @InjectMock
    public ActiveSyncRepository acRepository;

    @InjectMock
    public EngagementRepository eRepository;

    @InjectMock
    @RestClient
    public LodeStarGitApiClient gitApiClient;

    @InjectMock
    @RestClient
    public LodeStarStatusApiClient statusApiClient;

    @InjectMock
    @RestClient
    public ConfigApiClient configApiClient;
    
    @InjectMock
    @RestClient
    public ActivityApiClient activityClient;
    
    @InjectMock
    @RestClient
    public ArtifactApiClient artifactClient;
    
    @InjectMock
    @RestClient
    public ParticipantApiClient participantClient;

    public static String[] nullEmptyBlankSource() {
        return new String[] { null, "", "   " };
    }

}