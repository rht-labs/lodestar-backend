package com.redhat.labs.lodestar.model.search;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Filters.or;

import java.util.Optional;

import javax.ws.rs.WebApplicationException;

import org.bson.conversions.Bson;

import com.redhat.labs.lodestar.model.Engagement;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class StateSearchComponent extends RangeSearchComponent {

    private static final String INVALID_SEARCH_PARAMS = "'state' search parameter requires 'state', 'start', and 'end' search parameter to be provided.";

    private EngagementState state;

    /**
     * Returns an {@link Optional} containing a {@link Bson} where for
     * {@link Engagement}s using the range start and end dates as well as the state.
     */
    @Override
    public Optional<Bson> getBson() {

        validate();
        return Optional.ofNullable(createSearchBson());

    }

    /**
     * Throws a {@link WebApplicationException} if state, start or end is not set.
     */
    @Override
    void validate() {

        if (null == state) {
            throw new WebApplicationException(INVALID_SEARCH_PARAMS, 400);
        }
        super.validate();
    }

    /**
     * Returns a {@link Bson} for the configured state.
     * 
     * @return
     */
    private Bson createSearchBson() {

        Bson launched = null;
        Bson bson = null;

        if (!EngagementState.UPCOMING.equals(state)) {

            launched = exists(LAUNCH, true);

            if (EngagementState.ACTIVE.equals(state)) {
                bson = createActiveBson();
            } else if (EngagementState.PAST.equals(state)) {
                bson = createPastBson();
            } else {
                bson = createTerminatingBson();
            }

            return and(launched, bson);

        } else {
            return createUpcomingBson();
        }

    }

    /**
     * Creates a {@link Bson} for active engagements.
     * 
     * @return
     */
    private Bson createActiveBson() {
        return gte(ENGAGEMENT_END_DATE, super.getStart());
    }

    /**
     * Creates a {@link Bson} for past engagements.
     * 
     * @return
     */
    private Bson createPastBson() {
        return lt(ENGAGEMENT_END_DATE, super.getStart());
    }

    /**
     * Creates a {@link Bson} for terminating engagements.
     * 
     * @return
     */
    private Bson createTerminatingBson() {
        Bson endDateLtEnd = lt(ENGAGEMENT_END_DATE, super.getStart());
        Bson archiveDateGtEnd = gt(ENGAGEMENT_ARCHIVE_DATE, super.getStart());
        return and(endDateLtEnd, archiveDateGtEnd);
    }

    /**
     * Creates a {@link Bson} for upcoming engagements.
     * 
     * @return
     */
    private Bson createUpcomingBson() {
        return or(exists(LAUNCH, false), eq(LAUNCH, null));
    }

}
