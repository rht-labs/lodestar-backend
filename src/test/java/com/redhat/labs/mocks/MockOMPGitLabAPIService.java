package com.redhat.labs.mocks;

import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.omp.rest.client.OMPGitLabAPIService;

import io.quarkus.test.Mock;

@Mock
@RestClient
@ApplicationScoped
public class MockOMPGitLabAPIService implements OMPGitLabAPIService {

	private String foxyResponse = Json.createObjectBuilder().add("emoji", "\uD83E\uDD8A").build().toString();
	private String responseLocationHeader = "http://omp-git-api-omp-dev.apps.s11.core.rht-labs.com/api/residencies/12338";

	@Override
	public Response getFile(String name, String repoId) {
		return Response.status(Response.Status.OK).entity(foxyResponse).build();
	}

	@Override
	public Response createEngagement(Engagement engagement) {

		if (SCENARIO.SERVER_ERROR == SCENARIO.valueOf(engagement.getCustomerName())) {
			return Response.serverError().build();
		} else if (SCENARIO.RUNTIME_EXCEPTION == SCENARIO.valueOf(engagement.getCustomerName())) {
			throw new RuntimeException("uh oh");
		} else {
			return Response.status(Response.Status.CREATED).entity(foxyResponse)
					.header("Location", responseLocationHeader).build();
		}
	}

	public enum SCENARIO {

		SUCCESS("SUCCESS"), NOT_FOUND("NOT_FOUND"), SERVER_ERROR("SERVER_ERRROR"),
		RUNTIME_EXCEPTION("RUNTIME_EXCEPTION");

		private String value;

		SCENARIO(String value) {
			this.value = value;
		}

		public String getValue() {
			return this.value;
		}

	}

}
