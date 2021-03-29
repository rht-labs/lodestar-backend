package com.redhat.labs.lodestar.model.filter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import com.redhat.labs.lodestar.util.ClassFieldUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ListFilterOptions extends FilterOptions {

    @Parameter(name = "search", required = false, description = "search string used to query engagements.  allows =, like, not exists, exists")
    @QueryParam("search")
    private String search;

    @Parameter(name = "sortOrder", required = false, description = "response list sort order.  valid values are 'ASC' or 'DESC'")
    @QueryParam("sortOrder")
    private SortOrder sortOrder;

    @Parameter(name = "sortFields", required = false, description = "comma separated list of fields to sort on")
    @QueryParam("sortFields")
    private String sortFields;

    @Parameter(name = "page", required = false, description = "page number of results to return")
    @QueryParam("page")
    private Integer page;

    @Parameter(name = "perPage", required = false, description = "number of results per page to return")
    @QueryParam("perPage")
    private Integer perPage;

    @Builder.Default
    private Optional<String> suggestFieldName = Optional.empty();
    @Builder.Default
    private Optional<String> unwindFieldName = Optional.empty();
    @Builder.Default
    private Optional<String> unwindProjectFieldNames = Optional.empty();
    @Builder.Default
    private Optional<String> groupByFieldName = Optional.empty();

    public Optional<String> getSearch() {
        return Optional.ofNullable(search);
    }
   
    public Optional<SortOrder> getSortOrder() {
        return Optional.ofNullable(sortOrder);
    }

    public Optional<String> getSortFields() {
        return Optional.ofNullable(sortFields);
    }

    public Optional<Integer> getPage() {
        return Optional.ofNullable(page);
    }

    public Optional<Integer> getPerPage() {
        return Optional.ofNullable(perPage);
    }

    public List<String> getSortFieldsAsList() {
        String fields = getSortFields().orElse("customer_name,project_name");
        return Stream.of(fields.split(",")).map(ClassFieldUtils::snakeToCamelCase).collect(Collectors.toList());
    }

    public void addLikeSearchCriteria(String fieldName, String value) {
        addToSearchString(fieldName, value, false);
    }
    
    public void addEqualsSearchCriteria(String fieldName, String value) {
        addToSearchString(fieldName, value, true);
    }

    private void addToSearchString(String fieldName, String value, boolean exactMatch) {

        StringBuilder builder = getSearch().isPresent() ? new StringBuilder(search) : new StringBuilder();

        String[] split = value.split(",");
        String operator = exactMatch ? "=" : " like ";
        Stream.of(split).forEach(c -> builder.append("&").append(fieldName).append(operator).append(value));
        String newSearch = builder.toString();
        search = (newSearch.startsWith("&")) ? newSearch.substring(1) : newSearch;

    }

}