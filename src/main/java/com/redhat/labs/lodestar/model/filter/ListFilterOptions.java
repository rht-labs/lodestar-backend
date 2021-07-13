package com.redhat.labs.lodestar.model.filter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.HeaderParam;
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

    @Parameter(name = "Accept-version", required = false, description = "Valid Values are 'v1' or 'v2'. v2 pages results by default.  v1 sets per page to 500.")
    @HeaderParam(value = "Accept-version")
    String apiVersion;

    @Parameter(name = "search", required = false, description = "search string used to query engagements.  allows =, not like, like, not exists, exists")
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

    /**
     * Returns a {@link List} containing the fields specified in sort fields
     * attribute. If not provided, `customer_name,project_name` is used.
     * 
     * @return
     */
    public List<String> getSortFieldsAsList() {
        String fields = getSortFields().orElse("customer_name,project_name");
        return Stream.of(fields.split(",")).map(ClassFieldUtils::snakeToCamelCase).collect(Collectors.toList());
    }

    /**
     * Adds the provided field and value to the current search string as a Like
     * operator.
     * 
     * @param fieldName
     * @param value
     */
    public void addLikeSearchCriteria(String fieldName, String value) {
        addToSearchString(fieldName, value, false);
    }

    /**
     * Adds the provided field and value to the current search string as an Equals
     * operator.
     * 
     * @param fieldName
     * @param value
     */
    public void addEqualsSearchCriteria(String fieldName, String value) {
        addToSearchString(fieldName, value, true);
    }

    /**
     * Adds the field name and value to the search string. If exactMatch is true,
     * the operator is set to equals. Otherwise, the operator is Like.
     * 
     * @param fieldName
     * @param value
     * @param exactMatch
     */
    private void addToSearchString(String fieldName, String value, boolean exactMatch) {

        StringBuilder builder = getSearch().isPresent() ? new StringBuilder(search) : new StringBuilder();

        String[] split = value.split(",");
        String operator = exactMatch ? "=" : " like ";
        Stream.of(split).forEach(c -> builder.append("&").append(fieldName).append(operator).append(value));
        String newSearch = builder.toString();
        search = (newSearch.startsWith("&")) ? newSearch.substring(1) : newSearch;

    }

    /**
     * Parses the current search string for the search component of given field
     * name.
     * 
     * @param fieldName
     * @return
     */
    public Optional<String> getSearchStringByField(String fieldName) {

        if (getSearch().isEmpty()) {
            return getSearch();
        }

        return Stream.of(search.split("&")).filter(s -> s.startsWith(fieldName)).findFirst();

    }
    
    public Integer getPreviousPage() {
        return this.page == null || this.page == 1 ? null : this.page - 1;
    }
    
    public Integer getNextPage() {
        return this.page == null ? null : this.page + 1;
    }

}