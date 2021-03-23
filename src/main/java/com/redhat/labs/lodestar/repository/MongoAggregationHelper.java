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

import javax.ws.rs.WebApplicationException;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.BsonField;
import com.mongodb.client.model.Facet;
import com.mongodb.client.model.Field;
import com.redhat.labs.lodestar.model.filter.ListFilterOptions;
import com.redhat.labs.lodestar.model.filter.SortOrder;

public class MongoAggregationHelper {

    private static final String COUNT = "count";
    private static final String TO_LOWER_QUERY = "$toLower";

    private MongoAggregationHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static List<Bson> generateAggregationPipeline(ListFilterOptions filterOptions) {

        List<Bson> pipeline = new ArrayList<>();

        // unwind if required
        unwindStage(pipeline, filterOptions);

        // set match criteria
        matchStage(pipeline, filterOptions);

        // add lowercase field and group/count
        addLowercaseFieldAndGroupStage(pipeline, filterOptions);

        // sort results
        sortStage(pipeline, filterOptions);

        // enable paging or limits
        skipAndLimitStages(pipeline, filterOptions);

        // add projection
        projectionStage(pipeline, filterOptions);

        return pipeline;

    }

    public static List<Bson> generatePagedAggregationPipeline(ListFilterOptions filterOptions) {

        List<Bson> pipeline = new ArrayList<>();
        pipeline.addAll(queryPipeline(filterOptions));
        pipeline.addAll(pagingAndLimitFacetPipeline(filterOptions));
        pipeline.addAll(pagingProjectionPipeline());

        return pipeline;

    }

    private static List<Bson> queryPipeline(ListFilterOptions filterOptions) {

        // Create pipeline for search and sorting
        List<Bson> pipeline = new ArrayList<>();

        // unwind if required
        unwindStage(pipeline, filterOptions);

        // set match criteria
        matchStage(pipeline, filterOptions);

        // add lowercase field and group/count
        addLowercaseFieldAndGroupStage(pipeline, filterOptions);

        // sort results
        sortStage(pipeline, filterOptions);

        return pipeline;

    }

    private static List<Bson> pagingAndLimitFacetPipeline(ListFilterOptions filterOptions) {

        // create pipeline for paging and limits
        List<Bson> paging = new ArrayList<>();

        // enable paging or limits
        skipAndLimitStages(paging, filterOptions);

        // add projection
        projectionStage(paging, filterOptions);

        // Create facets for engagements and total count
        Facet engagements = new Facet("engagements", paging);
        Facet count = new Facet("totalEngagements", count());
        Bson facetStage = facet(engagements, count);

        return Arrays.asList(facetStage);

    }

    private static List<Bson> pagingProjectionPipeline() {

        // create documents for paging fields (engagements and totalEngagements)
        Document d = new Document("$arrayElemAt", Arrays.asList("$totalEngagements.count", 0));
        Document countField = new Document("totalEngagements", d);
        Document engagementsField = new Document("engagements", "$engagements");

        // projection for paging fields
        Bson pageProjection = project(fields(engagementsField, countField));

        return Arrays.asList(pageProjection);

    }

    private static void unwindStage(List<Bson> pipeline, ListFilterOptions filterOptions) {

        Optional<String> unwindFieldName = filterOptions.getUnwindFieldName();
        if (unwindFieldName.isPresent()) {
            pipeline.add(unwind(new StringBuilder("$").append(unwindFieldName.get()).toString()));
        }

    }

    private static void matchStage(List<Bson> pipeline, ListFilterOptions filterOptions) {

        Optional<String> search = filterOptions.getSearch();
        if (search.isPresent()) {
            Optional<Bson> bson = MongoHelper.buildSearchBson(search);
            if (bson.isPresent()) {
                pipeline.add(match(bson.get()));
            }
        }

    }

    private static void addLowercaseFieldAndGroupStage(List<Bson> pipeline, ListFilterOptions filterOptions) {

        Optional<String> addFieldName = filterOptions.getGroupByFieldName();
        if (addFieldName.isPresent()) {

            String fieldName = addFieldName.get();
            String fieldNameVar = new StringBuilder("$").append(fieldName).toString();

            String toLowerFieldName = new StringBuilder(fieldName).append("_lower").toString();
            String toLowerFieldNameVar = new StringBuilder("$").append(toLowerFieldName).toString();

            Document toLowerDocument = new Document(TO_LOWER_QUERY, fieldNameVar);
            pipeline.add(addFields(new Field<>(toLowerFieldName, toLowerDocument)));

            // Group by lowercase field and count
            BsonField[] fields = new BsonField[] { Accumulators.addToSet(getNestedFieldName(fieldName), fieldNameVar),
                    Accumulators.sum(COUNT, 1) };
            pipeline.add(group(toLowerFieldNameVar, fields));

        }

    }

    private static void sortStage(List<Bson> pipeline, ListFilterOptions filterOptions) {

        List<String> sortFields = filterOptions.getSortFieldsAsList();
        Bson sort = sort(MongoHelper.determineSort(filterOptions.getSortOrder().orElse(SortOrder.ASC),
                sortFields.toArray(new String[sortFields.size()])));
        pipeline.add(sort);

    }

    private static void skipAndLimitStages(List<Bson> pipeline, ListFilterOptions filterOptions) {

        Optional<Integer> page = filterOptions.getPage();
        Optional<Integer> perPage = filterOptions.getPerPage();

        if (page.isPresent()) {
            Integer pageNumber = page.get();
            Integer pageSize = perPage.isPresent() ? perPage.get() : 20;
            pipeline.add(skip(pageSize * (pageNumber - 1)));
            pipeline.add(limit(pageSize));
        }

    }

    private static void projectionStage(List<Bson> pipeline, ListFilterOptions filterOptions) {

        Optional<String> groupByField = filterOptions.getGroupByFieldName();
        Optional<Set<String>> include = filterOptions.getIncludeList();
        Optional<Set<String>> exclude = filterOptions.getExcludeList();

        if (groupByField.isPresent()) {
            String fieldName = getNestedFieldName(groupByField.get());
            pipeline.add(project(new Document("_id", 0).append(fieldName, "$_id").append(COUNT, "$count")));
        } else if (include.isPresent() && exclude.isPresent()) {
            throw new WebApplicationException("cannot provide both include and exclude parameters", 400);
        } else if (include.isPresent()) {
            pipeline.add(project(include(List.copyOf(include.get()))));
        } else if (exclude.isPresent()) {
            pipeline.add(project(exclude(List.copyOf(exclude.get()))));
        } else {
            pipeline.add(project(fields(excludeId())));
        }

    }

    private static String getNestedFieldName(String fieldName) {

        if (fieldName.contains(".")) {
            return fieldName.substring(fieldName.lastIndexOf(".") + 1);
        }

        return fieldName;

    }

}