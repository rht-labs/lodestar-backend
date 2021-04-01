package com.redhat.labs.lodestar.repository;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Filters.lte;
import static com.mongodb.client.model.Filters.regex;
import static com.mongodb.client.model.Filters.not;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.WebApplicationException;

import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Sorts;
import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.filter.EngagementState;
import com.redhat.labs.lodestar.model.filter.SortOrder;
import com.redhat.labs.lodestar.util.ClassFieldUtils;

public class MongoHelper {

    private MongoHelper() {
        throw new IllegalStateException("Utility class");
    }

    // fields
    private static final String LAUNCH = "launch";
    private static final String END_DATE = "endDate";

    // search keywords
    private static final String EXISTS = "exists";

    static Bson determineSort(SortOrder sortOrder, String... fieldNames) {
        return SortOrder.ASC.equals(sortOrder) ? Sorts.ascending(fieldNames) : Sorts.descending(fieldNames);
    }

    static Optional<Bson> buildSearchBson(Optional<String> search) {

        if (search.isEmpty()) {
            return Optional.empty();
        }

        Bson bson = null;
        String searchString = search.get();
        // parse search string
        String[] components = search.get().split("&");

        if (searchString.contains("state=")) {

            Optional<String> state = findInComponentArray(components, "state");
            Optional<String> today = findInComponentArray(components, "today");

            bson = findByState(state, today);

        } else {

            List<Bson> searchBson = Stream.of(components).map(MongoHelper::createBsonFromSearchComponent)
                    .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
            if (!searchBson.isEmpty()) {
                bson = and(searchBson);
            }

        }

        return (null == bson) ? Optional.empty() : Optional.of(bson);

    }

    static Optional<String> findInComponentArray(String[] components, String fieldName) {
        return Stream.of(components).filter(c -> c.contains(fieldName)).map(c -> c.split("=")[1]).findFirst();
    }

    static Optional<Bson> createBsonFromSuggestionComponent(String fieldName, Object value) {
        return (null == fieldName || null == value) ? Optional.empty() : Optional.of(eq(fieldName, value));
    }

    static Optional<Bson> createBsonFromSearchComponent(String component) {

        if (component.contains("=")) {
            String[] split = component.split("=");
            String fieldName = ClassFieldUtils.getFieldNameFromQueryName(split[0]);
            String value = split[1];
            if ("start".equals(fieldName)) {
                return Optional.of(isActiveAfterStart(value));
            } else if ("end".equals(fieldName)) {
                return Optional.of(isActiveBeforeEnd(value));
            } else {
                return Optional.of(eq(fieldName, getObjectFromString(fieldName, value)));
            }
        } else if (component.contains("like")) {
            boolean notRegex = component.contains("not");
            String splitRegex = notRegex ? "\\s+not\\s+like\\s+" : "\\s+like\\s+";
            String[] split = component.trim().split(splitRegex);
            String fieldName = ClassFieldUtils.getFieldNameFromQueryName(split[0]);
            String value = split[1];
            Bson bson = notRegex ? not(regex(fieldName, value, "i")) : regex(fieldName, value, "i");
            return Optional.of(bson);
        } else if (component.contains("not") && component.contains(EXISTS)) {
            String fieldName = component.replace("not", "").replace(EXISTS, "").trim();
            return Optional.of(exists(fieldName, false));
        } else if (component.contains(EXISTS)) {
            String fieldName = component.replace(EXISTS, "").trim();
            return Optional.of(exists(fieldName, true));
        } else {
            return Optional.empty();
        }

    }

    static Object getObjectFromString(String fieldName, String value) {

        Class<?> clazz = getTypeFromFieldName(Engagement.class, fieldName);
        if (null == clazz) {
            throw new WebApplicationException("could not get class for engagement.", 500);
        }
        try {
            Constructor<?> constructor = clazz.getConstructor(String.class);
            return constructor.newInstance(value);
        } catch (Exception e) {
            throw new WebApplicationException(
                    "cannot create instance of " + clazz.getName() + ", error = " + e.getMessage(), 400);
        }
    }

    static Class<?> getTypeFromFieldName(Class<?> clazz, String fieldName) {

        if (null == fieldName) {
            return null;
        }

        String current = fieldName;
        String nested = null;

        if (current.contains(".")) {
            current = fieldName.substring(0, fieldName.indexOf("."));
            nested = fieldName.substring(fieldName.indexOf(".") + 1);
        }

        try {

            Field f = clazz.getDeclaredField(current);
            Class<?> fieldClass = f.getType();

            if (nested == null) {
                return fieldClass;
            }

            Class<?> nextClass = f.getType();
            Type type = f.getGenericType();
            if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type;
                if (pt.getActualTypeArguments().length == 1) {
                    Type t = pt.getActualTypeArguments()[0];
                    nextClass = Class.forName(t.getTypeName());
                }
            }
            return getTypeFromFieldName(nextClass, nested);

        } catch (Exception e) {
            throw new WebApplicationException(String.format("invalid field %s on %s", fieldName, clazz.getName()), 400);
        }

    }

    static Bson isActiveAfterStart(String start) {

        Bson launchExists = launchedBson(true);
        Bson gte = gte("startDate", start);
        return and(launchExists, gte);

    }

    static Bson isActiveBeforeEnd(String end) {

        Bson launchExists = launchedBson(true);
        Bson lte = lte("startDate", end);
        return and(launchExists, lte);

    }

    static Bson launchedBson(boolean exists) {
        return exists(LAUNCH, exists);
    }

    static Bson findActiveEngagements(String today) {

        // launch is set
        Bson launchExists = launchedBson(true);
        // endDate >= today
        Bson gte = gte(END_DATE, today);
        return and(launchExists, gte);

    }

    static Bson findPastEngagements(String today) {

        // launched is set
        Bson launchExists = launchedBson(true);
        // endDate < today
        Bson lt = lt(END_DATE, today);
        return and(launchExists, lt);

    }

    static Bson findTerminatingEngagements(String today) {

        // launched is set
        Bson launchExists = launchedBson(true);
        // endDate < today < archiveDate
        Bson lt = lt(END_DATE, today);
        Bson gt = gt("archiveDate", today);
        return and(launchExists, lt, gt);

    }

    static Bson findUpcomingEngagements() {

        // launch is not set
        return launchedBson(false);

    }

    static Bson findByState(Optional<String> status, Optional<String> today) {

        String engagementStatus = status.orElse(EngagementState.UPCOMING.name());
        String localDate = today.orElse(LocalDate.now(ZoneId.of("Z")).toString());

        Bson bson;
        if (EngagementState.ACTIVE.name().equalsIgnoreCase(engagementStatus)) {
            bson = findActiveEngagements(localDate);
        } else if (EngagementState.TERMINATING.name().equalsIgnoreCase(engagementStatus)) {
            bson = findTerminatingEngagements(localDate);
        } else if (EngagementState.PAST.name().equalsIgnoreCase(engagementStatus)) {
            bson = findPastEngagements(localDate);
        } else {
            bson = findUpcomingEngagements();
        }

        return bson;

    }

    static Bson getUnwindProjectField(String fieldName) {

        String nestedFieldName = getNestedFieldName(fieldName);
        String fromVariableName = getVariableName(fieldName);
        return new BsonDocument(nestedFieldName, new BsonString(fromVariableName));

    }

    static String getLowercaseFieldName(String fieldName, boolean makeVariableName) {

        StringBuilder builder = new StringBuilder();

        if (makeVariableName) {
            builder.append("$");
        }

        return builder.append(fieldName).append("-lower").toString();

    }

    static String getNestedFieldName(String fieldName) {

        if (fieldName.contains(".")) {
            return fieldName.substring(fieldName.lastIndexOf(".") + 1);
        }

        return fieldName;

    }

    static String getVariableName(String fieldName) {
        return new StringBuilder("$").append(fieldName).toString();
    }

}