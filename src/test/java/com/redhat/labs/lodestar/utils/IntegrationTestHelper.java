package com.redhat.labs.lodestar.utils;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.PropertyNamingStrategy;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.redhat.labs.lodestar.repository.ActiveSyncRepository;
import com.redhat.labs.lodestar.repository.EngagementRepository;
import com.redhat.labs.lodestar.rest.client.LodeStarConfigApiClient;
import com.redhat.labs.lodestar.rest.client.LodeStarGitLabAPIService;
import com.redhat.labs.lodestar.rest.client.LodeStarStatusApiClient;

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
	public LodeStarGitLabAPIService gitApiClient;

	@InjectMock
	@RestClient
	public LodeStarStatusApiClient statusApiClient;

	@InjectMock
	@RestClient
	public LodeStarConfigApiClient configApiClient;

	public static String[] nullEmptyBlankSource() {
		return new String[] { null, "", "   " };
	}

}