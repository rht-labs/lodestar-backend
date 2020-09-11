package com.redhat.labs.lodestar.rest.client;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.redhat.labs.lodestar.model.status.VersionManifestV1;
import com.redhat.labs.lodestar.rest.client.LodeStarStatusApiClient;
import com.redhat.labs.lodestar.utils.ResourceLoader;

import io.quarkus.test.Mock;

@Mock
@RestClient
@ApplicationScoped
public class MockLodeStarStatusApiClient implements LodeStarStatusApiClient {

    @Inject
    Jsonb jsonb;

    @Override
    public VersionManifestV1 getVersionManifestV1() {

        String json = ResourceLoader.load("status-service/version-manifest.yaml");
        return jsonb.fromJson(json, VersionManifestV1.class);

    }

    @Override
    public Response getComponentStatus() {
        // TODO Auto-generated method stub
        return null;
    }

}
