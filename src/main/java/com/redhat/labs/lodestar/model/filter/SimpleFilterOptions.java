package com.redhat.labs.lodestar.model.filter;

import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimpleFilterOptions {

    @Parameter(name = "suggest", required = false, description = "uses suggestion as case insensitive search string")
    @QueryParam("suggest")
    private String suggest;

    @Parameter(name = "sortOrder", required = false, description = "response list sort order.  valid values are 'ASC' or 'DESC'")
    @QueryParam("sortOrder")
    private SortOrder sortOrder;

    @Parameter(name = "page", required = false, description = "page number of results to return")
    @QueryParam("page")
    private Integer page;

    @Parameter(name = "perPage", required = false, description = "number of results per page to return")
    @QueryParam("perPage")
    private Integer perPage;

    public ListFilterOptions from(String suggestFieldName) {

        ListFilterOptions options = new ListFilterOptions();

        sortOrder = (SortOrder.DESC.equals(sortOrder)) ? SortOrder.DESC : SortOrder.ASC;
        options.setSortOrder(sortOrder);

        Integer number = (null == page) ? 1 : page;
        options.setPage(number);

        Integer size = (null == perPage) ? 20 : perPage;
        options.setPerPage(size);

        if (null != suggest) {
            options.addLikeSearchCriteria(suggestFieldName, suggest);
        }

        return options;

    }

}
