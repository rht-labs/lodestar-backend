package com.redhat.labs.omp.repository;

import static com.mongodb.client.model.Filters.eq;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import org.bson.conversions.Bson;

import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.omp.model.FileAction;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class EngagementRepository implements PanacheMongoRepository<Engagement> {

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
     * @return
     */
    public Optional<Engagement> replaceEngagementIfLastUpdateMatched(Engagement replacement, String lastUpdate) {

        // create bson filter for last update
        Bson filter = eq("lastUpdate", lastUpdate);
        return Optional.ofNullable(this.mongoCollection().findOneAndReplace(filter, replacement));

    }

}
