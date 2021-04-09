package com.redhat.labs.lodestar.model.search;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Filters.lte;

import java.util.Optional;

import javax.ws.rs.WebApplicationException;

import org.bson.conversions.Bson;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class StateSearchComponent extends RangeSearchComponent {

    private static final String INVALID_SEARCH_PARAMS = "'state' search parameter requires 'state', 'start', and 'end' search parameter to be provided.";
    private static final String START_DATE = "startDate";
    private static final String END_DATE = "endDate";
    private static final String ARCHIVE_DATE = "archiveDate";

    private EngagementState state;

    @Override
    public Optional<Bson> getBson() {

        validate();
        return Optional.ofNullable(createSearchBson());

    }

    private void validate() {

        if (null == state || (null == super.getStart() || null == super.getEnd())) {
            throw new WebApplicationException(INVALID_SEARCH_PARAMS, 400);
        }
    }

    private Bson createSearchBson() {

        Bson launched = null;
        Bson bson = null;

        if (!EngagementState.UPCOMING.equals(state)) {

            launched = exists("launch", true);

            if (EngagementState.ACTIVE.equals(state)) {
                bson = createActiveBson();
            } else if (EngagementState.PAST.equals(state)) {
                bson = createPastBson();
            } else {
                bson = createTerminatingBson();
            }

        } else {
            launched = exists("launch", false);
            bson = createUpcomingBson();
        }

        return and(launched, bson);

    }

    private Bson createActiveBson() {
        return gte(END_DATE, super.getEnd());
    }

    private Bson createPastBson() {
        return lt(END_DATE, super.getEnd());
    }

    private Bson createTerminatingBson() {
        Bson endDateLtEnd = lt(END_DATE, super.getEnd());
        Bson archiveDateGtEnd = gt(ARCHIVE_DATE, super.getEnd());
        return and(endDateLtEnd, archiveDateGtEnd);
    }

    private Bson createUpcomingBson() {
        Bson startDateGteEnd = gte(START_DATE, super.getStart());
        Bson startDateLteEnd = lte(START_DATE, super.getEnd());
        return and(startDateGteEnd, startDateLteEnd);

    }

}
