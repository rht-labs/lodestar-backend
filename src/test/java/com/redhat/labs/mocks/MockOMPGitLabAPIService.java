package com.redhat.labs.mocks;

import com.redhat.labs.OMPGitLabAPIService;
import io.quarkus.test.Mock;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.ws.rs.core.Response;


@Mock
@ApplicationScoped
@RestClient
public class MockOMPGitLabAPIService implements OMPGitLabAPIService {

    private String foxyResponse = Json.createObjectBuilder().add("emoji", "\uD83E\uDD8A").build().toString();

    @Override
    public Response getFile(String name, String repoId) {
        return Response.status(Response.Status.OK).entity(foxyResponse).build();
    }

    @Override
    public Response createNewResidency(Object residency) {
        return Response.status(Response.Status.OK).entity(foxyResponse).build();
    }
}
