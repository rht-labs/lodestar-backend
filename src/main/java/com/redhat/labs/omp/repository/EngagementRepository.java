package com.redhat.labs.omp.repository;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import org.bson.conversions.Bson;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.omp.model.FileAction;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class EngagementRepository implements PanacheMongoRepository<Engagement> {

    public static final List<String> IMMUTABLE_FIELDS = new ArrayList<>(
            Arrays.asList("mongoId", "projectId", "creationDetails", "status", "commits", "launch"));

    private ObjectMapper objectMapper = new ObjectMapper();

    public Engagement findByEngagementId(Integer engagementId) {
        return find("engagementId", engagementId).firstResult();
    }

    public Engagement findByCustomerNameAndProjectName(String customerName, String projectName) {
        return find("customerName=?1 and projectName=?2", customerName, projectName).firstResult();
    }

    public List<Engagement> findByModifiedAndAction(FileAction action) {
        return find("action", action).list();
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
     * Returns an {@link Optional} containing the updated {@link Engagement} where
     * last update matched. Otherwise, returns an empty {@link Optional}
     * 
     * @param replacement
     * @param lastUpdate
     * @param skipLaunch
     * @return
     */
    public Optional<Engagement> updateEngagementIfLastUpdateMatched(Engagement toUpdate, String lastUpdate,
            boolean skipLaunch) {

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

        return and(eq("customerName", engagement.getCustomerName()), eq("projectName", engagement.getProjectName()),
                eq("lastUpdate", lastUpdate));

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
