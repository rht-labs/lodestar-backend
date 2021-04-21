package com.redhat.labs.lodestar.repository;

import static com.mongodb.client.model.Aggregates.addFields;
import static com.mongodb.client.model.Aggregates.count;
import static com.mongodb.client.model.Aggregates.facet;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.limit;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Aggregates.skip;
import static com.mongodb.client.model.Aggregates.sort;
import static com.mongodb.client.model.Aggregates.unwind;
import static com.mongodb.client.model.Projections.exclude;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.ws.rs.WebApplicationException;

import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.BsonField;
import com.mongodb.client.model.Facet;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Sorts;
import com.redhat.labs.lodestar.model.filter.ListFilterOptions;
import com.redhat.labs.lodestar.model.filter.SortOrder;
import com.redhat.labs.lodestar.model.search.BsonSearch;

public class MongoAggregationHelper {

    private static final String COUNT = "count";
    private static final String TOTAL_COUNT = "totalCount";
    private static final String TO_LOWER_QUERY = "$toLower";
    private static final String ARRAY_ELEMENT_AT = "$arrayElemAt";

    private MongoAggregationHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static List<Bson> generatePagedAggregationPipeline(ListFilterOptions filterOptions) {

        List<Bson> pipeline = new ArrayList<>();
        pipeline.addAll(queryPipeline(filterOptions));
        pipeline.addAll(pagingAndLimitPipeline(filterOptions));
        pipeline.addAll(pagingProjectionPipeline());

        return pipeline;

    }

    public static List<Bson> generatePagedAggregationPipelineForUserSummary(ListFilterOptions filterOptions) {

        List<Bson> pipeline = new ArrayList<>();

        pipeline.addAll(queryPipeline(filterOptions));
        pipeline.addAll(userSummaryFacets());
        pipeline.addAll(userSummaryProjection());

        return pipeline;

    }

    static List<Bson> queryPipeline(ListFilterOptions filterOptions) {

        // Create pipeline for search and sorting
        List<Bson> pipeline = new ArrayList<>();

        // set match criteria
        matchStage(pipeline, filterOptions);

        // unwind if required
        unwindStage(pipeline, filterOptions);

        // add lowercase field and group/count
        addLowercaseFieldAndGroupStage(pipeline, filterOptions);

        // sort results
        sortStage(pipeline, filterOptions);

        return pipeline;

    }

    static List<Bson> pagingAndLimitPipeline(ListFilterOptions filterOptions) {

        // create pipeline for paging and limits
        List<Bson> paging = new ArrayList<>();

        // enable paging or limits
        skipAndLimitStages(paging, filterOptions);

        // add projection
        projectionStage(paging, filterOptions);

        // Create facets for engagements and total count
        Facet engagements = new Facet("results", paging);
        Facet count = new Facet(TOTAL_COUNT, count());
        Bson facetStage = facet(engagements, count);

        return Arrays.asList(facetStage);

    }

    static List<Bson> userSummaryFacets() {

        Facet count = new Facet(TOTAL_COUNT, count());

        // match on rh users
        List<Bson> rhUserPipeline = new ArrayList<>();
        matchStage(rhUserPipeline, ListFilterOptions.builder().search("email like @redhat.com").build());
        rhUserPipeline.add(count());
        Facet rhUsers = new Facet("rhUsers", rhUserPipeline);

        // match on rh users
        List<Bson> otherUserPipeline = new ArrayList<>();
        matchStage(otherUserPipeline, ListFilterOptions.builder().search("email not like @redhat.com").build());
        otherUserPipeline.add(count());
        Facet otherUsers = new Facet("otherUsers", otherUserPipeline);

        Bson facetStage = facet(count, rhUsers, otherUsers);

        return Arrays.asList(facetStage);

    }

    static List<Bson> userSummaryProjection() {

        Document allUsersCount = new Document(ARRAY_ELEMENT_AT, Arrays.asList("$totalCount.count", 0));
        Document allUsersCountField = new Document("allUsersCount", allUsersCount);
        Document rhUsersCount = new Document(ARRAY_ELEMENT_AT, Arrays.asList("$rhUsers.count", 0));
        Document rhUsersCountField = new Document("rhUsersCount", rhUsersCount);
        Document otherUsersCount = new Document(ARRAY_ELEMENT_AT, Arrays.asList("$otherUsers.count", 0));
        Document otherUsersCountField = new Document("otherUsersCount", otherUsersCount);

        Bson summaryProjection = project(fields(allUsersCountField, rhUsersCountField, otherUsersCountField));
        return Arrays.asList(summaryProjection);

    }

    static List<Bson> pagingProjectionPipeline() {

        // create documents for paging fields (engagements and totalEngagements)
        Document d = new Document(ARRAY_ELEMENT_AT, Arrays.asList("$totalCount.count", 0));
        Document countField = new Document(TOTAL_COUNT, d);
        Document engagementsField = new Document("results", "$results");

        // projection for paging fields
        Bson pageProjection = project(fields(engagementsField, countField));

        return Arrays.asList(pageProjection);

    }

    /*
     * Stages
     */

    static void unwindStage(List<Bson> pipeline, ListFilterOptions filterOptions) {

        Optional<String> unwindFieldName = filterOptions.getUnwindFieldName();
        if (unwindFieldName.isEmpty()) {
            return;
        }

        // unwind based on field name
        pipeline.add(unwind(getVariableName(unwindFieldName.get())));

        // get unwind field search string
        Optional<String> search = filterOptions.getSearchStringByField(unwindFieldName.get());
        if (search.isPresent()) {
            // reset search string
            filterOptions.setSearch(search.get());
            // match on unwind search string
            matchStage(pipeline, filterOptions);
        }

        Optional<String> projectFields = filterOptions.getUnwindProjectFieldNames();
        if (projectFields.isEmpty()) {
            return;
        }

        // create include fields from fields
        String[] fields = projectFields.get().split(",");
        Bson[] excludeId = new Bson[] { excludeId() };
        Bson[] engagementUuid = new Bson[] { new BsonDocument("engagementUuid", new BsonString("$uuid")) };
        Bson[] bsonFields = Stream.of(fields).map(MongoAggregationHelper::getUnwindProjectField).toArray(Bson[]::new);
        Bson[] combined = Stream.of(excludeId, bsonFields, engagementUuid).flatMap(Stream::of).toArray(Bson[]::new);

        // project fields after unwind
        pipeline.add(project(fields(combined)));

    }

    static void matchStage(List<Bson> pipeline, ListFilterOptions filterOptions) {

        Optional<String> searchString = filterOptions.getSearch();
        if (searchString.isPresent()) {
            BsonSearch search = BsonSearch.builder().searchString(searchString.get()).build();
            Optional<Bson> bson = search.createBsonForSearch();
            if (bson.isPresent()) {
                pipeline.add(match(bson.get()));
            }
        }

    }

    // requires groupbyfield
    static void addLowercaseFieldAndGroupStage(List<Bson> pipeline, ListFilterOptions filterOptions) {

        Optional<String> groupByFieldName = filterOptions.getGroupByFieldName();
        if (groupByFieldName.isPresent()) {

            String fieldName = groupByFieldName.get();
            String fieldNameVar = getVariableName(fieldName);

            String toLowerFieldName = getLowercaseFieldName(fieldName);
            String toLowerFieldNameVar = getVariableName(toLowerFieldName);

            Document toLowerDocument = new Document(TO_LOWER_QUERY, fieldNameVar);
            pipeline.add(addFields(new Field<>(toLowerFieldName, toLowerDocument)));

            // Group by lowercase field and count
            BsonField[] fields = new BsonField[] { // Accumulators.addToSet(getNestedFieldName(fieldName),
                                                   // fieldNameVar),
                    Accumulators.first("root", "$$ROOT"), Accumulators.sum(COUNT, 1) };

            pipeline.add(group(toLowerFieldNameVar, fields));

            pipeline.add(Aggregates
                    .replaceRoot(new Document("$mergeObjects", Arrays.asList("$root", new Document(COUNT, "$count")))));
        }

    }

    static void sortStage(List<Bson> pipeline, ListFilterOptions filterOptions) {

        List<String> sortFields = filterOptions.getSortFieldsAsList();
        Bson sort = sort(determineSort(filterOptions.getSortOrder().orElse(SortOrder.ASC),
                sortFields.toArray(new String[sortFields.size()])));
        pipeline.add(sort);

    }

    static void skipAndLimitStages(List<Bson> pipeline, ListFilterOptions filterOptions) {

        Optional<Integer> page = filterOptions.getPage();
        Optional<Integer> perPage = filterOptions.getPerPage();

        if (page.isPresent()) {
            Integer pageNumber = page.get();
            Integer pageSize = perPage.isPresent() ? perPage.get() : 20;
            pipeline.add(skip(pageSize * (pageNumber - 1)));
            pipeline.add(limit(pageSize));
        }

    }

    static void projectionStage(List<Bson> pipeline, ListFilterOptions filterOptions) {

        Optional<String> groupByField = filterOptions.getGroupByFieldName();
        Optional<Set<String>> include = filterOptions.getIncludeList();
        Optional<Set<String>> exclude = filterOptions.getExcludeList();

        if (include.isPresent() && exclude.isPresent()) {
            throw new WebApplicationException("cannot provide both include and exclude parameters", 400);
        } else if (include.isPresent()) {
            pipeline.add(project(fields(excludeId(), include(List.copyOf(include.get())))));
        } else if (exclude.isPresent()) {
            pipeline.add(project(fields(excludeId(), exclude(List.copyOf(exclude.get())))));
        } else if (groupByField.isPresent()) {
            String fieldName = getNestedFieldName(groupByField.get());
            pipeline.add(project(fields(excludeId(),
                    new Document().append(fieldName, "$" + groupByField.get()).append(COUNT, "$count"))));
        } else {
            pipeline.add(project(fields(excludeId())));
        }

    }

    /**
     * 
     * Returns a sort {@link Bson} for the given {@link SortOrder} and sort fields.
     * 
     * @param sortOrder
     * @param fieldNames
     * @return
     */
    static Bson determineSort(SortOrder sortOrder, String... fieldNames) {
        return SortOrder.ASC.equals(sortOrder) ? Sorts.ascending(fieldNames) : Sorts.descending(fieldNames);
    }

    /**
     * Creates a {@link BsonDocument} for the nested field name and sources the
     * value from the field name.
     * 
     * @param fieldName
     * @return
     */
    static Bson getUnwindProjectField(String fieldName) {

        String nestedFieldName = getNestedFieldName(fieldName);
        String fromVariableName = getVariableName(fieldName);
        return new BsonDocument(nestedFieldName, new BsonString(fromVariableName));

    }

    /**
     * Appends '-lower' to the given field name.
     * 
     * @param fieldName
     * @return
     */
    static String getLowercaseFieldName(String fieldName) {
        return new StringBuilder(fieldName).append("-lower").toString();
    }

    /**
     * Returns the field name after the last '.'.
     * 
     * @param fieldName
     * @return
     */
    static String getNestedFieldName(String fieldName) {

        if (fieldName.contains(".")) {
            return fieldName.substring(fieldName.lastIndexOf(".") + 1);
        }

        return fieldName;

    }

    /**
     * Appends a '$' to the given field name.
     * 
     * @param fieldName
     * @return
     */
    static String getVariableName(String fieldName) {
        return new StringBuilder("$").append(fieldName).toString();
    }

}