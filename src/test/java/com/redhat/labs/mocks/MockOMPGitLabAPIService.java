package com.redhat.labs.mocks;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.redhat.labs.omp.model.git.api.GitApiEngagement;
import com.redhat.labs.omp.model.git.api.GitApiFile;
import com.redhat.labs.omp.rest.client.OMPGitLabAPIService;

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
    public Response createEngagement(GitApiEngagement engagement, String username, String userEmail) {

        if (SCENARIO.SUCCESS.value.equalsIgnoreCase(engagement.getDescription())) {

            String location = "some/path/to/id/1234";
            return Response.status(201).header("Location", location).build();

        } else if (SCENARIO.RUNTIME_EXCEPTION.value.equalsIgnoreCase(engagement.getDescription())) {

            return Response.serverError().build();

        } else if (SCENARIO.SERVER_ERROR.value.equalsIgnoreCase(engagement.getDescription())) {

            return Response.status(500).build();

        }

        return null;
    }

    @Override
    public Response createFile(Integer projectId, GitApiFile file) {
        return null;
    }

    @Override
    public Response updateFile(Integer projectId, GitApiFile file) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public GitApiFile getFile(Integer projectId, String filePath) {
        return GitApiFile.builder().filePath("somefile.txt").content("some file context here").build();
    }

    @Override
    public Response deleteFile(Integer projectId, String filePath, String username, String userEmail) {
        // TODO Auto-generated method stub
        return null;
    }

}
