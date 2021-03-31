package com.redhat.labs.lodestar.model.pagination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;

import org.junit.jupiter.api.Test;

class PagedResultsTest {

    @Test
    void testGetHeadersWithHeadersNull() {

        PagedResults results = PagedResults.builder().build();
        results.setHeaders(null);

        Map<String, Object> headers =  results.getHeaders();
        assertNotNull(headers);

        assertTrue(headers.containsKey("x-current-page"));
        assertEquals(1, headers.get("x-current-page"));

        assertTrue(headers.containsKey("x-per-page"));
        assertEquals(20, headers.get("x-per-page"));

        assertTrue(headers.containsKey("x-first-page"));
        assertEquals(1, headers.get("x-first-page"));

        assertFalse(headers.containsKey("x-next-page"));

        assertTrue(headers.containsKey("x-last-page"));
        assertEquals(1, headers.get("x-last-page"));

    }

    @Test
    void testGetHeadersWithHeadersEmpty() {

        PagedResults results = PagedResults.builder().build();
        results.setHeaders(new HashMap<>());

        Map<String, Object> headers =  results.getHeaders();
        assertNotNull(headers);

        assertTrue(headers.containsKey("x-current-page"));
        assertEquals(1, headers.get("x-current-page"));

        assertTrue(headers.containsKey("x-per-page"));
        assertEquals(20, headers.get("x-per-page"));

        assertTrue(headers.containsKey("x-first-page"));
        assertEquals(1, headers.get("x-first-page"));

        assertFalse(headers.containsKey("x-next-page"));

        assertTrue(headers.containsKey("x-last-page"));
        assertEquals(1, headers.get("x-last-page"));

    }

    @Test
    void testGetHeaders() {

        PagedResults results = PagedResults.builder().currentPage(1).perPage(1).totalCount(3).build();

        Map<String, Object> headers =  results.getHeaders();
        assertNotNull(headers);

        assertTrue(headers.containsKey("x-current-page"));
        assertEquals(1, headers.get("x-current-page"));

        assertTrue(headers.containsKey("x-per-page"));
        assertEquals(1, headers.get("x-per-page"));

        assertTrue(headers.containsKey("x-first-page"));
        assertEquals(1, headers.get("x-first-page"));

        assertTrue(headers.containsKey("x-next-page"));
        assertEquals(2, headers.get("x-next-page"));

        assertTrue(headers.containsKey("x-last-page"));
        assertEquals(3, headers.get("x-last-page"));

    }

    @Test
    void testGetHeadersOnLastPage() {

        PagedResults results = PagedResults.builder().currentPage(3).perPage(1).totalCount(3).build();

        Map<String, Object> headers =  results.getHeaders();
        assertNotNull(headers);

        assertTrue(headers.containsKey("x-current-page"));
        assertEquals(3, headers.get("x-current-page"));

        assertTrue(headers.containsKey("x-per-page"));
        assertEquals(1, headers.get("x-per-page"));

        assertTrue(headers.containsKey("x-first-page"));
        assertEquals(1, headers.get("x-first-page"));

        assertFalse(headers.containsKey("x-next-page"));

        assertTrue(headers.containsKey("x-last-page"));
        assertEquals(3, headers.get("x-last-page"));

    }
    
    @Test
    void testGetLinksFirstPage() {

        PagedResults results = PagedResults.builder().currentPage(1).perPage(1).totalCount(3).build();
        Link[] links = results.getLinks(UriBuilder.fromPath("http://some/path"));
        assertEquals(4, links.length);

    }

    @Test
    void testGetLinksLastPage() {

        PagedResults results = PagedResults.builder().currentPage(3).perPage(1).totalCount(3).build();
        Link[] links = results.getLinks(UriBuilder.fromPath("http://some/path"));
        assertEquals(3, links.length);

    }

    @Test
    void testGetLinksHeadersNull() {

        PagedResults results = PagedResults.builder().build();
        results.setHeaders(null);

        Link[] links = results.getLinks(UriBuilder.fromPath("http://some/path"));
        assertEquals(3, links.length);

    }

    @Test
    void testGetLinksHeadersEmpty() {

        PagedResults results = PagedResults.builder().build();
        results.setHeaders(new HashMap<>());

        Link[] links = results.getLinks(UriBuilder.fromPath("http://some/path"));
        assertEquals(3, links.length);

    }

}
