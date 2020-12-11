package com.redhat.labs.lodestar.repository;

import static com.mongodb.client.model.Aggregates.addFields;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Aggregates.sort;
import static com.mongodb.client.model.Aggregates.unwind;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.regex;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.enterprise.context.ApplicationScoped;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import com.redhat.labs.lodestar.model.Artifact;
import com.redhat.labs.lodestar.model.Category;
import com.redhat.labs.lodestar.model.Engagement;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class EngagementRepository implements PanacheMongoRepository<Engagement> {

    private static final List<String> IMMUTABLE_FIELDS = new ArrayList<>(
            Arrays.asList("uuid", "mongoId", "projectId", "creationDetails", "status", "commits", "launch"));

    private ObjectMapper objectMapper = new ObjectMapper();

    public Optional<Engagement> findByUiid(String uuid) {
        return find("uuid", uuid).firstResultOptional();
    }

    public Optional<Engagement> findByCustomerNameAndProjectName(String customerName, String projectName) {
        return find("customerName=?1 and projectName=?2", customerName, projectName).firstResultOptional();
    }

    public List<Engagement> findByModified() {
        return find("action is not null").list();
    }

    /**
     * A case insensitive string to match against customer names.
     * 
     * @param input
     * @return
     */
    public List<Engagement> findCustomerSuggestions(String input) {

        String queryInput = String.format("(?i)%s", input);
        return find("customerName like ?1", queryInput).list();

    }

    /**
     * A case insensitive string to match against category names.
     * 
     * @param input
     * @return
     */
    public List<Category> findCategorySuggestions(String input) {

        Iterable<Category> iterable = mongoCollection().aggregate(
                Arrays.asList(unwind("$categories"), match(regex("categories.name", String.format("(?i)%s", input))),
                        addFields(new Field<>("categories.lower_name", new Document("$toLower", "$categories.name"))),
                        group("$categories.lower_name", Accumulators.sum("count", 1)),
                        project(new Document("_id", 0).append("name", "$_id").append("count", "$count")),
                        sort(Sorts.descending("count"))),
                Category.class);

        return StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());

    }

    /**
     * Returns all unique {@link Category} with associated counts.
     * 
     * @return
     */
    public List<Category> findAllCategoryWithCounts() {

        Iterable<Category> iterable = mongoCollection().aggregate(Arrays.asList(unwind("$categories"),
                addFields(new Field<>("categories.lower_name", new Document("$toLower", "$categories.name"))),
                group("$categories.lower_name", Accumulators.sum("count", 1)),
                project(new Document("_id", 0).append("name", "$_id").append("count", "$count")),
                sort(Sorts.descending("count"))), Category.class);

        return StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());

    }

    /**
     * Returns all artifact types that match the provides {@link String}.
     * 
     * @param input
     * @return
     */
    public List<String> findArtifactTypeSuggestions(String input) {

        // get all types that match the input string
        Iterable<Artifact> iterable = mongoCollection()
                .aggregate(
                        Arrays.asList(unwind("$artifacts"),
                                match(regex("artifacts.type", String.format("(?i)%s", input))),
                                addFields(new Field<>("artifacts.lower_type",
                                        new Document("$toLower", "$artifacts.type"))),
                                group("$artifacts.lower_type"), project(new Document().append("type", "$_id")),
                                sort(Sorts.ascending("type"))),
                        Artifact.class);

        return StreamSupport.stream(iterable.spliterator(), false).map(artifact -> artifact.getType())
                .collect(Collectors.toList());

    }

    /**
     * Returns all unique artifact types.
     * 
     * @return
     */
    public List<String> findAllArtifactTypes() {

        // get all unique artifact types
        Iterable<Artifact> iterable = mongoCollection()
                .aggregate(
                        Arrays.asList(unwind("$artifacts"),
                                addFields(new Field<>("artifacts.lower_type",
                                        new Document("$toLower", "$artifacts.type"))),
                                group("$artifacts.lower_type"), project(new Document().append("type", "$_id")),
                                sort(Sorts.ascending("type"))),
                        Artifact.class);

        return StreamSupport.stream(iterable.spliterator(), false).map(artifact -> artifact.getType())
                .collect(Collectors.toList());

    }

    /**
     * Find {@link Engagement} with a {@link Category} name that matches the given
     * {@link String}.
     * 
     * @param name
     * @param matchCase
     * @return
     */
    public List<Engagement> findEngagementsByCategory(String name, boolean matchCase) {
        String queryInput = matchCase ? String.format("/^%s$/", name) : String.format("/^%s$/i", name);
        return find("categories.name like ?1", queryInput).list();
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
            if (!f.equals("launch") || skipLaunch) {
                fieldMap.remove(f);
            }
        });

        // add a set for each field in the update
        for (String key : fieldMap.keySet()) {

            Object value = fieldMap.get(key);
            Bson update = set(key, value);

            if (null == updates) {
                updates = update;
            } else {
                updates = combine(updates, update);
            }

        }

        return updates;

    }

}
