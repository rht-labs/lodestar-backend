package com.redhat.labs.omp.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.omp.exception.ResourceAlreadyExistsException;
import com.redhat.labs.omp.exception.ResourceNotFoundException;
import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.omp.model.git.api.GitApiEngagement;
import com.redhat.labs.omp.model.git.api.GitApiFile;
import com.redhat.labs.omp.repository.EngagementRepository;
import com.redhat.labs.omp.rest.client.OMPGitLabAPIService;

@ApplicationScoped
public class EngagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EngagementService.class);

    @ConfigProperty(name = "engagement.file.name", defaultValue = "engagement.json")
    String engagementFileName;
    @ConfigProperty(name = "engagement.file.branch", defaultValue = "master")
    String engagementFileBranch;
    @ConfigProperty(name = "engagement.file.commit.message", defaultValue = "updated by omp backend")
    String engagementFileCommitMessage;

    @Inject
    Jsonb jsonb;

    @Inject
    @RestClient
    OMPGitLabAPIService gitApi;

    @Inject
    EngagementRepository repository;

    /**
     * Creates a new {@link Engagement} resource in the data store and then
     * asynchronously sends a request to create the resource using the Git API
     * service.
     * 
     * @param engagement
     * @return
     */
    public Engagement create(Engagement engagement) {

        // check if engagement exists
        Optional<Engagement> optional = get(engagement.getCustomerName(), engagement.getProjectName());
        if (optional.isPresent()) {
            throw new ResourceAlreadyExistsException("engagement already exists.  use PUT to update resource.");
        }

        // persist to db
        repository.persist(engagement);

        LOGGER.info("sending to git api. " + engagement);
        // send to gitlab for processing
        gitApi.createEngagement(GitApiEngagement.from(engagement))
                .whenComplete((response, throwable) -> updateEngagementId(engagement.id, response, throwable));

        return engagement;

    }

    /**
     * Updates the {@link Engagement} resource in the data store and then
     * asynchronously sends a request to update the resource using the Git API
     * service.
     * 
     * @param customerName
     * @param projectName
     * @param engagement
     * @return
     */
    public Engagement update(String customerName, String projectName, Engagement engagement) {

        // check if engagement exists
        Optional<Engagement> optional = get(customerName, projectName);
        if (!optional.isPresent()) {
            throw new ResourceNotFoundException("no engagement found.  use POST to create resource.");
        }

        // set id to persisted id
        engagement.id = optional.get().id;
        // update in db
        repository.update(engagement);

        // validate project id is available
        if (null == engagement.getEngagementId()) {
            throw new IllegalStateException("engagement exists, but engagement id is missing.");
        }

        // commit new version to gitlab
        GitApiFile file = createFileFromEngagement(engagement);

        // async update file using git api service
        CompletableFuture.runAsync(() -> updateFile(file, engagement.getEngagementId()));

        return engagement;

    }

    /**
     * Returns an {@link Optional} containing an {@link Engagement} if it is present
     * in the data store. Otherwise, an empty {@link Optional} is returned.
     * 
     * @param customerId
     * @param projectId
     * @return
     */
    public Optional<Engagement> get(String customerName, String projectName) {

        Optional<Engagement> optional = Optional.empty();

        // check db
        Engagement persistedEngagement = repository.findByCustomerNameAndProjectName(customerName, projectName);

        if (null != persistedEngagement) {
            optional = Optional.of(persistedEngagement);
        }

        return optional;

    }

    /**
     * Returns a {@link List} of all {@link Engagement} in the data store.
     * 
     * @return
     */
    public List<Engagement> getAll() {
        return repository.listAll();
    }

    /**
     * Removes the {@link Engagement} from the data store and sends a request
     * asychronously to the git api to remove from gitlab.
     * 
     * @param customerName
     * @param projectName
     */
    public void delete(String customerName, String projectName) {

        // check if engagement exists
        Optional<Engagement> optional = get(customerName, projectName);

        if (!optional.isPresent()) {
            throw new ResourceNotFoundException("no engagement found.  use POST to create resource.");
        }

        Engagement engagement = optional.get();

        // async delete file from gitlab
        CompletableFuture.runAsync(() -> deleteFile(engagement.getEngagementId(), engagementFileName));

        // remove from db
        repository.delete(optional.get());

    }

    /**
     * Removes all {@link Engagement} from the data store
     */
    public void deleteAll() {
        long count = repository.deleteAll();
        LOGGER.info("removed '" + count + "' engagements from the data store.");
    }

    /**
     * Helper function to create {@link GitApiFile} from an {@link Engagement}.
     * 
     * @param engagement
     * @return
     */
    private GitApiFile createFileFromEngagement(Engagement engagement) {
        return GitApiFile.builder().filePath(engagementFileName).branch(engagementFileBranch)
                .commitMessage(engagementFileCommitMessage).content(jsonb.toJson(engagement)).build();
    }

    /**
     * Callback function to handle response from the asynchronous REST call to
     * create a new {@link Engagement} using the Git API service. This method will
     * update the data store using the engagement ID returned in the
     * {@link Response}.
     * 
     * @param id
     * @param response
     * @param throwable
     */
    private void updateEngagementId(ObjectId id, Response response, Throwable throwable) {

        LOGGER.info("updating engagement id for mongo id '" + id.toString() + "'");

        if (null != throwable || response.getStatus() != HttpStatus.SC_CREATED) {
            LOGGER.error("exception occurred during REST call. '" + throwable.getMessage() + "'");
            // do something if exception
        }

        String location = response.getHeaderString("Location");
        String engagementId = location.substring(location.lastIndexOf("/") + 1);

        // get engagement by object id
        LOGGER.info("looking for engagment with id '" + id + "'");
        Engagement engagement = repository.findById(id);

        // update engagement id
        LOGGER.info("adding id '" + engagementId + "' to engagement '" + engagement);
        engagement.setEngagementId(Integer.valueOf(engagementId));

        repository.update(engagement);

    }

    private void deleteFile(Integer engagementId, String filePath) {

        // call git api to delete file
        gitApi.deleteFile(engagementId, filePath);

    }

    private void updateFile(GitApiFile file, Integer engagementId) {

        LOGGER.info("updating file for engagement id {}", engagementId);

        try {
            // does file exist?
            gitApi.getFile(engagementId, file.getFilePath());
        } catch (WebApplicationException e) {
            // create the file if it was not found
            if (HttpStatus.SC_NOT_FOUND == e.getResponse().getStatus()) {
                Response created = gitApi.createFile(engagementId, file);
                LOGGER.info("create response status {}", created.getStatus());
            } else {
                throw e;
            }
        }

        // run update
        Response updated = gitApi.updateFile(engagementId, file);
        LOGGER.info("update response status {}", updated.getStatus());

    }

    private void insertEngagementListInRepository(List<Engagement> engagementList) {
        repository.persist(engagementList);
    }

    public void syncWithGitLab() {

        // get all engagements from git api
        // TODO: Get list from Git API
        List<Engagement> engagementList = new ArrayList<>();

        // remove all from database
        deleteAll();

        // insert
        insertEngagementListInRepository(engagementList);

    }

}
