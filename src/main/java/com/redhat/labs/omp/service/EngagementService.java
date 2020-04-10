package com.redhat.labs.omp.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.omp.exception.ResourceAlreadyExistsException;
import com.redhat.labs.omp.exception.ResourceNotFoundException;
import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.omp.model.git.api.FileAction;
import com.redhat.labs.omp.model.git.api.GitApiFile;
import com.redhat.labs.omp.repository.EngagementRepository;

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

        // set modified info
        engagement.setModified(true);
        engagement.setAction(FileAction.create);

        // persist to db
        repository.persist(engagement);

        return engagement;

    }

    /**
     * Updates the {@link Engagement} resource in the data store.
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

        // mark as updated
        engagement.setModified(true);
        engagement.setAction(FileAction.update);

        // update in db
        repository.update(engagement);

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

        LOGGER.info("{}", repository.listAll());
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
     * Marks the {@link Engagement} for deletion. The Git API sync process will
     * perform the delete from the data store.
     * 
     * @param customerName
     * @param projectName
     * @param user
     * @param userEmail
     */
    public void delete(String customerName, String projectName, String user, String userEmail) {

        // check if engagement exists
        Optional<Engagement> optional = get(customerName, projectName);

        if (!optional.isPresent()) {
            throw new ResourceNotFoundException("no engagement found.  use POST to create resource.");
        }

        Engagement engagement = optional.get();

        // mark as modified for delete by sync process
        engagement.setModified(true);
        engagement.setAction(FileAction.delete);

        // update in repository
        repository.persist(engagement);

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
    public GitApiFile createFileFromEngagement(Engagement engagement) {
        return GitApiFile.builder().filePath(engagementFileName).branch(engagementFileBranch)
                .commitMessage(engagementFileCommitMessage).content(jsonb.toJson(engagement))
                .authorName(engagement.getLastUpdateByName()).authorEmail(engagement.getLastUpdateByEmail()).build();
    }

    /**
     * Callback function to handle response from the asynchronous REST call to
     * create a new {@link Engagement} using the Git API service. This method will
     * update the data store using the engagement ID returned in the
     * {@link Response}.
     * 
     * @param engagement
     * @param response
     * @param throwable
     */
    public void updateEngagementId(Engagement engagement, Response response) {

        LOGGER.info("updating engagement id for mongo id '" + engagement.id + "'");

        String location = response.getHeaderString("Location");
        String engagementId = location.substring(location.lastIndexOf("/") + 1);

        // update engagement id
        LOGGER.info("adding id '" + engagementId + "' to engagement '" + engagement);
        engagement.setEngagementId(Integer.valueOf(engagementId));

        repository.update(engagement);

    }

    /**
     * Persists the {@link List} of {@link Engagement} into the data store.
     * 
     * @param engagementList
     */
    private void insertEngagementListInRepository(List<Engagement> engagementList) {
        repository.persist(engagementList);
    }

    /**
     * Updates teh {@link List} of {@link Engagement} in the data store.
     * 
     * @param engagementList
     */
    public void updateEngagementListInRepository(List<Engagement> engagementList) {
        repository.update(engagementList);
    }

    /**
     * Responsible for purging the data store of all {@link Engagement} and then
     * calling the Git API to repopulate the data store with data from Git API.
     */
    public void syncWithGitLab() {

        // get all engagements from git api
        // TODO: Get list from Git API
        List<Engagement> engagementList = new ArrayList<>();

        // remove all from database
        deleteAll();

        // insert
        insertEngagementListInRepository(engagementList);

    }

    /**
     * Returns a {@link List} of {@link Engagement} where the modified flag is set
     * to true and the action is set to the given {@link FileAction}
     * 
     * @return
     */
    public List<Engagement> getModifiedEngagementsByAction(FileAction action) {
        return repository.findByModifiedAndAction(action);
    }

}
