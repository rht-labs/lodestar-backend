package com.redhat.labs.mocks;

import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.omp.rest.client.OMPGitLabAPIService;

import io.quarkus.test.Mock;
import lombok.Setter;

@Mock
@RestClient
@ApplicationScoped
public class MockOMPGitLabAPIService implements OMPGitLabAPIService {

	private String foxyResponse = Json.createObjectBuilder().add("emoji", "\uD83E\uDD8A").build().toString();

	@Override
	public Response getFile(String name, String repoId) {
		return Response.status(Response.Status.OK).entity(foxyResponse).build();
	}

	@Override
	public Response createEngagement(Engagement engagement) {
		return Response.status(Response.Status.OK).entity(foxyResponse).build();
	}

}
