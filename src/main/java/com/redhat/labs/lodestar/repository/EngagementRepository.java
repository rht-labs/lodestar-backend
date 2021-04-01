package com.redhat.labs.lodestar.repository;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.regex;
import static com.mongodb.client.model.Projections.exclude;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.enterprise.context.ApplicationScoped;

import org.bson.conversions.Bson;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.redhat.labs.lodestar.model.Artifact;
import com.redhat.labs.lodestar.model.Commit;
import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.EngagementUserSummary;
import com.redhat.labs.lodestar.model.Status;
import com.redhat.labs.lodestar.model.filter.FilterOptions;
import com.redhat.labs.lodestar.model.filter.ListFilterOptions;
import com.redhat.labs.lodestar.model.filter.SimpleFilterOptions;
import com.redhat.labs.lodestar.model.pagination.PagedArtifactResults;
import com.redhat.labs.lodestar.model.pagination.PagedCategoryResults;
import com.redhat.labs.lodestar.model.pagination.PagedEngagementResults;
import com.redhat.labs.lodestar.model.pagination.PagedStringResults;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class EngagementRepository implements PanacheMongoRepository<Engagement> {

    private static final String CUSTOMER_NAME = "customerName";
    private static final String PROJECT_NAME = "projectName";
    private static final String NAME = "name";
    private static final String CATEGORIES = "categories";
    private static final String CATEGORIES_NAME = new StringBuilder(CATEGORIES).append(".").append(NAME).toString();
    private static final String TYPE = "type";
    private static final String ARTIFACTS = "artifacts";
    private static final String ARTIFACTS_TYPE = new StringBuilder(ARTIFACTS).append(".").append(TYPE).toString();
    private static final String COUNT = "count";
    private static final String LAUNCH = "launch";

    private static final List<String> IMMUTABLE_FIELDS = new ArrayList<>(
            Arrays.asList("uuid", "mongoId", "projectId", "creationDetails", "status", "commits", LAUNCH));

    private ObjectMapper objectMapper = new ObjectMapper();

    /*
     * 
     * SET Methods
     * 
     */

    /**
     * Returns an {@link Optional} containing the updated {@link Engagement} where
     * the uuid matched. Otherwise, returns an empty {@link Optional}.
     * 
     * @param uuid
     * @param projectId
     * @return
     */
    public Optional<Engagement> setProjectId(String uuid, Integer projectId) {

        Bson filter = eq("uuid", uuid);
        Bson update = set("projectId", projectId);

        FindOneAndUpdateOptions optionAfter = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);

        return Optional.ofNullable(this.mongoCollection().findOneAndUpdate(filter, update, optionAfter));

    }

    /**
     * Sets the {@link Status} for the given UUID.
     * 
     * @param uuid
     * @param status
     * @return
     */
    public Optional<Engagement> setStatus(String uuid, Status status) {

        Bson filter = eq("uuid", uuid);
        Bson update = set("status", status);

        FindOneAndUpdateOptions optionAfter = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);

        return Optional.ofNullable(this.mongoCollection().findOneAndUpdate(filter, update, optionAfter));

    }

    /**
     * Sets the {@link Commit}s for the given UUID.
     * 
     * @param uuid
     * @param commits
     * @return
     */
    public Optional<Engagement> setCommits(String uuid, List<Commit> commits) {

        Bson filter = eq("uuid", uuid);
        Bson update = set("commits", commits);

        FindOneAndUpdateOptions optionAfter = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);

        return Optional.ofNullable(this.mongoCollection().findOneAndUpdate(filter, update, optionAfter));

    }

    /**
     * Returns an {@link Optional} containing the updated {@link Engagement} where
     * last update matched. Otherwise, returns an empty {@link Optional}
     * 
     * @param replacement
     * @param lastUpdate
     * @param skipLaunch
     * @return
     */
    public Optional<Engagement> updateEngagementIfLastUpdateMatched(Engagement toUpdate, String lastUpdate,
            Boolean skipLaunch) {

        // create the bson for filter and update
        Bson filter = createFilterForEngagement(toUpdate, lastUpdate);
        Bson update = createUpdateDocument(toUpdate, skipLaunch);

        FindOneAndUpdateOptions optionAfter = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);

        return Optional.ofNullable(this.mongoCollection().findOneAndUpdate(filter, update, optionAfter));

    }

    /*
     * 
     * GET Optional<Engagement Methods
     * 
     */

    /**
     * Returns Optional containing an {@link Engagement} that matches the provided
     * subdomain.
     * 
     * @param subdomain
     * @return
     */
    public Optional<Engagement> findBySubdomain(String subdomain) {
        return findBySubdomain(subdomain, Optional.empty());
    }

    public Optional<Engagement> findBySubdomain(String subdomain, Optional<String> engagementUuid) {

        String regex = new StringBuilder("^").append(subdomain).append("$").toString();
        Bson filter = regex("hostingEnvironments.ocpSubDomain", regex, "im");

        if (engagementUuid.isPresent()) {
            filter = and(filter, eq("uuid", engagementUuid.get()));
        }

        return Optional.ofNullable(mongoCollection().find(filter).first());

    }

    /**
     * Returns an {@link Optional} containing the {@link Engagement} with the UUID.
     * Otherwise, an empty {@link Optional} is returned.
     * 
     * @param uuid
     * @return
     */
    public Optional<Engagement> findByUuid(String uuid) {
        return findByUuid(uuid, new FilterOptions());
    }

    /**
     * Returns an {@link Optional} containing the {@link Engagement} with the UUID.
     * Otherwise, an empty {@link Optional} is returned.
     * 
     * If FilterOptions is provided, the associated projection will be used.
     * Otherwise, all fields will be returned.
     * 
     * @param uuid
     * @param filterOptions
     * @return
     */
    public Optional<Engagement> findByUuid(String uuid, FilterOptions filterOptions) {
        Bson bson = eq("uuid", uuid);
        return Optional.ofNullable(find(Optional.of(bson), filterOptions).first());
    }

    /**
     * Returns an {@link Optional} containing the {@link Engagement} with the
     * customer and project names. Otherwise, an empty {@link Optional} is returned.
     * 
     * @param customerName
     * @param projectName
     * @return
     */
    public Optional<Engagement> findByCustomerNameAndProjectName(String customerName, String projectName) {
        return findByCustomerNameAndProjectName(customerName, projectName, new FilterOptions());
    }

    /**
     * Returns an {@link Optional} containing the {@link Engagement} with the
     * customer and project names. Otherwise, an empty {@link Optional} is returned.
     * 
     * If FilterOptions is provided, the associated projection will be used.
     * Otherwise, all fields will be returned.
     * 
     * @param customerName
     * @param projectName
     * @param filterOptions
     * @return
     */
    public Optional<Engagement> findByCustomerNameAndProjectName(String customerName, String projectName,
            FilterOptions filterOptions) {
        Bson bson = and(eq(CUSTOMER_NAME, customerName), eq(PROJECT_NAME, projectName));
        return Optional.ofNullable(find(Optional.of(bson), filterOptions).first());
    }

    /*
     * 
     * GET List<Engagement> Methods
     * 
     */

    /**
     * Returns all {@link Engagement}s.
     * 
     * If FilterOptions is provided, the associated projection will be used.
     * Otherwise, all fields will be returned.
     * 
     * @param filterOptions
     * @return
     */
    public PagedEngagementResults findAll(ListFilterOptions filterOptions) {

        List<Bson> pipeline = MongoAggregationHelper.generatePagedAggregationPipeline(filterOptions);
        Optional<PagedEngagementResults> optional = findFirstFromIterable(
                mongoCollection().aggregate(pipeline, PagedEngagementResults.class));

        PagedEngagementResults results = optional
                .orElse(PagedEngagementResults.builder().results(Arrays.asList()).build());
        results.setCurrentPage(filterOptions.getPage().orElse(1));
        results.setPerPage(filterOptions.getPerPage().orElse(20));

        return results;

    }

    /**
     * Returns {@link PagedEngagementResults} of results for the given
     * {@link ListFilterOptions}.
     * 
     * @param filterOptions
     * @return
     */
    public PagedEngagementResults findPagedEngagements(ListFilterOptions filterOptions) {

        List<Bson> pipeline = MongoAggregationHelper.generatePagedAggregationPipeline(filterOptions);
        Optional<PagedEngagementResults> optional = findFirstFromIterable(
                mongoCollection().aggregate(pipeline, PagedEngagementResults.class));
        PagedEngagementResults page = optional.orElse(PagedEngagementResults.builder().build());

        page.setCurrentPage(filterOptions.getPage().orElse(1));
        page.setPerPage(filterOptions.getPerPage().orElse(20));

        return page;

    }

    /*
     * 
     * GET List of Other Objects Methods
     * 
     */

    /**
     * A case insensitive string to match against customer names.
     * 
     * @param input
     * @return
     */
    public PagedStringResults findCustomerSuggestions(SimpleFilterOptions options) {

        ListFilterOptions lfo = options.from(CUSTOMER_NAME);

        // set options for group by and sort
        lfo.setInclude(CUSTOMER_NAME);
        lfo.setGroupByFieldName(Optional.of(CUSTOMER_NAME));
        lfo.setSortFields(MongoHelper.getLowercaseFieldName("customer_name", false));

        List<Bson> pipeline = MongoAggregationHelper.generatePagedAggregationPipeline(lfo);

        Optional<PagedEngagementResults> optional = findFirstFromIterable(
                mongoCollection().aggregate(pipeline, PagedEngagementResults.class));
        PagedEngagementResults engagementResults = optional
                .orElse(PagedEngagementResults.builder().results(Arrays.asList()).build());

        // get customer names from results
        List<String> customerNames = engagementResults.getResults().stream().map(Engagement::getCustomerName)
                .collect(Collectors.toList());

        PagedStringResults results = PagedStringResults.builder().totalCount(engagementResults.getTotalCount())
                .results(customerNames).build();
        if (null != options.getPage()) {
            results.setCurrentPage(options.getPage());
        }
        if (null != options.getPerPage()) {
            results.setPerPage(options.getPerPage());
        }

        return results;

    }

    public PagedCategoryResults findCategories(SimpleFilterOptions options) {

        ListFilterOptions lfo = options.from(CATEGORIES_NAME);

        lfo.setUnwindFieldName(Optional.of(CATEGORIES));
        lfo.setGroupByFieldName(Optional.of(CATEGORIES_NAME));
        lfo.setSortFields(COUNT);

        List<Bson> pipeline = MongoAggregationHelper.generatePagedAggregationPipeline(lfo);

        Optional<PagedCategoryResults> optional = findFirstFromIterable(
                mongoCollection().aggregate(pipeline, PagedCategoryResults.class));
        PagedCategoryResults page = optional.orElse(PagedCategoryResults.builder().results(Arrays.asList()).build());

        page.setCurrentPage(options.getPage() != null ? options.getPage() : 1);
        page.setPerPage(options.getPerPage() != null ? options.getPerPage() : 20);

        return page;

    }

    public PagedStringResults findArtifactTypes(SimpleFilterOptions options) {

        ListFilterOptions lfo = options.from(ARTIFACTS_TYPE);

        lfo.setUnwindFieldName(Optional.of(ARTIFACTS));
        lfo.setUnwindProjectFieldNames(Optional.of(ARTIFACTS_TYPE));
        lfo.setGroupByFieldName(Optional.of(TYPE));
        lfo.setSortFields(TYPE);

        List<Bson> pipeline = MongoAggregationHelper.generatePagedAggregationPipeline(lfo);

        Optional<PagedArtifactResults> optional = findFirstFromIterable(
                mongoCollection().aggregate(pipeline, PagedArtifactResults.class));
        PagedArtifactResults engagementResults = optional
                .orElse(PagedArtifactResults.builder().results(Arrays.asList()).build());

        // get customer names from results
        List<String> customerNames = engagementResults.getResults().stream().map(Artifact::getType)
                .collect(Collectors.toList());

        PagedStringResults results = PagedStringResults.builder().totalCount(engagementResults.getTotalCount())
                .results(customerNames).build();
        if (null != options.getPage()) {
            results.setCurrentPage(options.getPage());
        }
        if (null != options.getPerPage()) {
            results.setPerPage(options.getPerPage());
        }

        return results;

    }

    public EngagementUserSummary findEngagementUserSummary(ListFilterOptions filterOptions) {

        filterOptions.setUnwindFieldName(Optional.of("engagementUsers"));
        filterOptions.setUnwindProjectFieldNames(Optional.of("engagementUsers.email"));
        filterOptions.setGroupByFieldName(Optional.of("email"));
        filterOptions.setSortFields("email");

        List<Bson> pipeline = MongoAggregationHelper.generatePagedAggregationPipelineForUserSummary(filterOptions);
        Optional<EngagementUserSummary> optional = findFirstFromIterable(
                mongoCollection().aggregate(pipeline, EngagementUserSummary.class));

        return optional.isPresent() ? optional.get() : EngagementUserSummary.builder().build();

    }

    /*
     * 
     * Helper Methods
     * 
     */

    /**
     * Returns a {@link Bson} containing the filter to find {@link Engagement} with
     * the corresponding customer name, project name, and last update timestamp.
     * 
     * @param engagement
     * @param lastUpdate
     * @return
     */
    private Bson createFilterForEngagement(Engagement engagement, String lastUpdate) {
        return and(eq("uuid", engagement.getUuid()), eq("lastUpdate", lastUpdate));
    }

    /**
     * Returns a {@link Bson} containing the fields to be updated for a given
     * {@link Engagement}.
     * 
     * @param engagement
     * @param skipLaunch
     * @return
     */
    private Bson createUpdateDocument(Engagement engagement, boolean skipLaunch) {

        Bson updates = null;

        // convert to map
        TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {
        };
        Map<String, Object> fieldMap = objectMapper.convertValue(engagement, typeRef);

        // remove values that should not be updated
        IMMUTABLE_FIELDS.forEach(f -> {
            if (!f.equals(LAUNCH) || skipLaunch) {
                fieldMap.remove(f);
            }
        });

        // add a set for each field in the update
        for (Entry<String, Object> entry : fieldMap.entrySet()) {

            Bson update = set(entry.getKey(), entry.getValue());

            if (null == updates) {
                updates = update;
            } else {
                updates = combine(updates, update);
            }

        }

        return updates;

    }

    /**
     * Returns a FindIterable with the resulting {@link Bson} filter or all if no
     * filter provided. A projection is added if either the include or exclude is
     * prvided in the FilterOptions.
     * 
     * @param bson
     * @param filterOptions
     * @return
     */
    private FindIterable<Engagement> find(Optional<Bson> bson, FilterOptions filterOptions) {

        Optional<Set<String>> includeSet = filterOptions.getIncludeList();
        Optional<Set<String>> excludeSet = filterOptions.getExcludeList();

        // return only the attributes to include
        if (includeSet.isPresent()) {
            return getFindIterable(bson).projection(include(List.copyOf(includeSet.get())));
        }

        // return only the attributes not excluded
        if (excludeSet.isPresent()) {
            return getFindIterable(bson).projection(exclude(List.copyOf(excludeSet.get())));
        }

        // return full engagement if no filter
        return getFindIterable(bson);

    }

    /**
     * Returns a FindIterable for all {@link Engagement}s or the results of the
     * {@link Bson} filter.
     * 
     * @param bson
     * @return
     */
    private FindIterable<Engagement> getFindIterable(Optional<Bson> bson) {

        if (bson.isPresent()) {
            return mongoCollection().find(bson.get());
        }

        return mongoCollection().find();

    }

    /*
     * Helper Methods
     */

    private <T> Optional<T> findFirstFromIterable(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false).findFirst();
    }

}