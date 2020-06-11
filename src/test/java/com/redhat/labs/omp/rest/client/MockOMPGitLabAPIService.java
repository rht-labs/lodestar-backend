package com.redhat.labs.omp.rest.client;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.omp.model.Status;
import com.redhat.labs.omp.model.Version;

import io.quarkus.test.Mock;

@Mock
@RestClient
@ApplicationScoped
public class MockOMPGitLabAPIService implements OMPGitLabAPIService {

    public enum SCENARIO {

        SUCCESS("SUCCESS"), NOT_FOUND("NOT_FOUND"), SERVER_ERROR("SERVER_ERRROR"),
        RUNTIME_EXCEPTION("RUNTIME_EXCEPTION"), FOUND("FOUND");

        private String value;

        SCENARIO(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }

    }

    @Override
    public Response createOrUpdateEngagement(Engagement engagement, String username, String userEmail) {

        if (SCENARIO.SUCCESS.value.equalsIgnoreCase(engagement.getDescription())) {

            String location = "some/path/to/id/1234";
            return Response.status(201).header("Location", location).build();

        } else if (SCENARIO.RUNTIME_EXCEPTION.value.equalsIgnoreCase(engagement.getDescription())) {

            throw new WebApplicationException("uh oh");

        } else if (SCENARIO.SERVER_ERROR.value.equalsIgnoreCase(engagement.getDescription())) {

            return Response.status(500).build();

        }

        return null;
    }

    @Override
    public Response getConfigFile() {
        String file = "{ \"content\": \"content\", \"encoding\": \"base64\", \"file_path\": \"myfile.yaml\" }";
        return Response.ok(file).build();
    }

    @Override
    public Response getConfigFileV2() {
		String json = "{ \"hello\" : \"world\" }";
		return Response.ok(json).build();
    }

    @Override
    public List<Engagement> getEngagments() {
        List<Engagement> engagementList = new ArrayList<>();

        engagementList.add(Engagement.builder().customerName("anotherCustomer").projectName("anotherProject")
                .projectId(4321).build());

        return engagementList;

    }

    @Override
    public Version getVersion() {
        Version v = Version.builder().gitCommit("abcdef").gitTag("v1.1").build();
        return v;
    }

	@Override
	public Status getStatus(String customer, String engagement) {
		if("exists".equals(engagement)) {
			return Status.builder().status("green").build();
		}
		return null;
	}

}
