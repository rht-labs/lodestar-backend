package com.redhat.labs.lodestar.resources;

import javax.inject.Inject;
import javax.json.bind.Jsonb;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.redhat.labs.lodestar.repository.ActiveSyncRepository;
import com.redhat.labs.lodestar.repository.EngagementRepository;
import com.redhat.labs.lodestar.rest.client.LodeStarGitLabAPIService;
import com.redhat.labs.lodestar.rest.client.LodeStarStatusApiClient;

import io.quarkus.test.junit.mockito.InjectMock;

public class EngagementResourceTestHelper {

    @Inject
    Jsonb quarkusJsonb;

    @InjectMock
    ActiveSyncRepository acRepository;

    @InjectMock
    EngagementRepository eRepository;

    @InjectMock
    @RestClient
    LodeStarGitLabAPIService gitApiClient;
    
    @InjectMock
    @RestClient
    LodeStarStatusApiClient statusApiClient;

    static String[] nullEmptyBlankSource() {
        return new String[] {null, "", "   "};
    }

}