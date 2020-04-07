package com.redhat.labs.omp.service;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.omp.exception.ResourceAlreadyExistsException;
import com.redhat.labs.omp.exception.ResourceNotFoundException;
import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.omp.model.File;
import com.redhat.labs.omp.repository.EngagementRepository;
import com.redhat.labs.omp.rest.client.OMPGitLabAPIService;

@ApplicationScoped
public class EngagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EngagementService.class);

    // TODO: Should be configurable
    private String engagementFileName;
    private String engagementFileBranch;
    private String engagementFileCommitMessage;

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

        // send to gitlab for processing
//        gitApi.createEngagement(engagement)
//                .whenComplete((response, throwable) -> updateEngagementId(engagement.id, response, throwable));

        return engagement;

    }

    /**
     * Updates the {@link Engagement} resource in the data store and then
     * asynchronously sends a request to update the resource using the Git API
     * service.
     * 
     * @param engagement
     * @return
     */
    public Engagement update(Engagement engagement) {

        // check if engagement exists
        Optional<Engagement> optional = get(engagement.getCustomerName(), engagement.getProjectName());
        if (!optional.isPresent()) {
            throw new ResourceNotFoundException("no engagement found.  use POST to create resource.");
        }

        // set id to persisted id
        engagement.id = optional.get().id;
        // update in db
        repository.update(engagement);

        // commit new version to gitlab
        File file = createFileFromEngagement(engagement);
        // TODO: This should be asynchronous
//        gitApi.updateFile(engagement.getEnagementId(), file);

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

    public Optional<Engagement> getById(String id) {

        Optional<Engagement> optional = Optional.empty();

        Engagement persistedEngagement = repository.findById(new ObjectId(id));

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

    public void delete(String customerName, String projectName) {

        // check if engagement exists
        Optional<Engagement> optional = get(customerName, projectName);

        if (!optional.isPresent()) {
            throw new ResourceNotFoundException("no engagement found.  use POST to create resource.");
        }

        // remove from db
        repository.delete(optional.get());

        // TODO: send request to remove from git lab

    }

    public void deleteById(String id) {

        // check if engagement exists
        Optional<Engagement> optional = getById(id);

        if (!optional.isPresent()) {
            throw new ResourceNotFoundException("no engagement found.  use POST to create resource.");
        }

        // remove from db
        repository.delete(optional.get());

        // TODO: send request to remove from git lab

    }

    /**
     * Removes all {@link Engagement} from the data store
     */
    public void deleteAll() {
        long count = repository.deleteAll();
        LOGGER.info("removed '" + count + "' engagements from the data store.");
    }

    /**
     * Helper function to create {@link File} from an {@link Engagement}.
     * 
     * @param engagement
     * @return
     */
    private File createFileFromEngagement(Engagement engagement) {
        return File.builder().filePath(engagementFileName).branch(engagementFileBranch)
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
        String engagementId = location.substring(location.lastIndexOf("/"));

        // get engagement by object id
        LOGGER.info("looking for engagment with id '" + id + "'");
        Engagement engagement = repository.findById(id);

        // update engagement id
        LOGGER.info("adding id '" + engagementId + "' to engagement '" + engagement);
        engagement.setEnagementId(Integer.valueOf(engagementId));

        repository.update(engagement);

    }

}
