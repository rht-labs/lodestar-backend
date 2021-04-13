package com.redhat.labs.lodestar.model.search;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.lte;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Filters.or;

import java.util.Optional;

import javax.ws.rs.WebApplicationException;

import org.bson.conversions.Bson;

import com.redhat.labs.lodestar.model.Engagement;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class RangeSearchComponent implements BsonSearchComponent {

    private static final String INVALID_SEARCH_PARAMS = "if either 'start', or 'end' search parameters specified, both need to be provided.";

    static final String LAUNCH = "launch";
    static final String LAUNCH_DATETIME = new StringBuilder(LAUNCH).append(".launchedDateTime").toString();
    static final String ENGAGEMENT_START_DATE = "endDate";
    static final String ENGAGEMENT_END_DATE = "endDate";
    static final String ENGAGEMENT_ARCHIVE_DATE = "archiveDate";
    

    private String start;
    private String end;

    /**
     * Returns an {@link Optional} containing a {@link Bson} where for
     * {@link Engagement}s that have been launched and the launch date or the end
     * date is within the range.
     */
    public Optional<Bson> getBson() {

        validate();

        // launched
        Bson launched = exists(LAUNCH, true);

        // engagement launch date between start and end
        Bson lDate = betweenStartAndEnd(LAUNCH_DATETIME, true);

        // engagement end date between start and end
        Bson eDate = betweenStartAndEnd(ENGAGEMENT_END_DATE, true);

        return Optional.of(and(launched, or(lDate, eDate)));
    }

    void validate() {

        if (null == start || null == end) {
            throw new WebApplicationException(INVALID_SEARCH_PARAMS, 400);
        }

    }

    /**
     * Returns a {@link Bson} where the given attribute is between the start and end
     * dates. Inclusive set to true will include start and end. Otherwise, start and
     * end will be excluded.
     * 
     * @param attribute
     * @param inclusive
     * @return
     */
    Bson betweenStartAndEnd(String attribute, boolean inclusive) {

        Bson dateGteStart = inclusive ? gte(attribute, start) : gt(attribute, start);
        Bson dateLteEnd = inclusive ? lte(attribute, end) : lt(attribute, end);

        return and(dateGteStart, dateLteEnd);

    }

}
