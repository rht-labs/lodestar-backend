package com.redhat.labs.lodestar.model.pagination;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResults {

    private static final String CURRENT_PAGE = "page";
    private static final String PER_PAGE = "per_page";
    private static final String PAGE_HEADER_FORMAT = "x-%s-page";
    private static final String PER_PAGE_HEADER = "x-per-page";

    private static final String CURRENT = "current";
    private static final String CURRENT_PAGE_HEADER = String.format(PAGE_HEADER_FORMAT, CURRENT);

    private static final String FIRST = "first";
    private static final String FIRST_PAGE_HEADER = String.format(PAGE_HEADER_FORMAT, FIRST);

    private static final String NEXT = "next";
    private static final String NEXT_PAGE_HEADER = String.format(PAGE_HEADER_FORMAT, NEXT);

    private static final String LAST = "last";
    private static final String LAST_PAGE_HEADER = String.format(PAGE_HEADER_FORMAT, LAST);

    @Builder.Default
    private Integer totalCount = 0;

    @Builder.Default
    private Integer currentPage = 1;
    @Builder.Default
    private Integer perPage = 20;

    @Builder.Default
    private Map<String, Map<String, Integer>> linkHeaders = new HashMap<>();
    @Builder.Default
    private Map<String, Object> headers = new HashMap<>();

    /**
     * Returns a {@link Map} containing the headers.
     * 
     * @return
     */
    public Map<String, Object> getHeaders() {

        if (null == headers || headers.isEmpty()) {
            setHeadersForRelations();
        }

        return headers;

    }

    /**
     * Creates an array lf {@link Link} for the configured link headers.
     * 
     * @param uriBuilder
     * @return
     */
    public Link[] getLinks(UriBuilder uriBuilder) {

        if (null == linkHeaders || linkHeaders.isEmpty()) {
            setHeadersForRelations();
        }

        List<Link> links = linkHeaders.entrySet().stream().map(e1 -> {

            String rel = e1.getKey();
            Map<String, Integer> headerMap = e1.getValue();

            javax.ws.rs.core.Link.Builder builder = Link.fromUriBuilder(uriBuilder).rel(rel);
            headerMap.entrySet().forEach(e2 -> builder.param(e2.getKey(), String.valueOf(e2.getValue())));

            return builder.build();

        }).collect(Collectors.toList());

        return links.toArray(new Link[links.size()]);

    }

    /**
     * Sets the Headers for the relations CURRENT, FIRST, LAST, and NEXT.
     */
    private void setHeadersForRelations() {

        if (null == headers) {
            headers = new HashMap<>();
        }

        // current page
        setHeadersForRelation(CURRENT, CURRENT_PAGE_HEADER, currentPage);

        // first page set to 1
        setHeadersForRelation(FIRST, FIRST_PAGE_HEADER, 1);

        // last page set to total / per page rounded up
        int totalPages = totalCount == 0 ? 1 : (int) Math.ceil((double) totalCount / perPage);
        setHeadersForRelation(LAST, LAST_PAGE_HEADER, totalPages);

        // set next if not greater than total
        if (currentPage + 1 <= totalPages) {
            setHeadersForRelation(NEXT, NEXT_PAGE_HEADER, currentPage + 1);
        }

        // set per page header
        headers.put(PER_PAGE_HEADER, perPage);

    }

    /**
     * Adds the header for the given relation to the associated {@link Map}s.
     * 
     * @param rel
     * @param pageHeader
     * @param pageValue
     */
    private void setHeadersForRelation(String rel, String pageHeader, Integer pageValue) {

        headers.put(pageHeader, pageValue);
        linkHeaders.put(rel, Map.of(CURRENT_PAGE, pageValue, PER_PAGE, perPage));

    }

}
