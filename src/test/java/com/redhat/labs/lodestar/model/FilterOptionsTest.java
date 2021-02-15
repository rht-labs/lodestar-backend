package com.redhat.labs.lodestar.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

class FilterOptionsTest {

    @Test
    void testGetIncludeListNull() {

        FilterOptions options = FilterOptions.builder().include(null).build();
        assertTrue(options.getIncludeList().isEmpty());

    }

    @Test
    void testGetIncludeListNotNull() {

        FilterOptions options = FilterOptions.builder().include("value,another_value").build();
        Optional<Set<String>> optional = options.getIncludeList();
        assertTrue(optional.isPresent());
        Set<String> set = optional.get();
        assertEquals(2, set.size());
        assertTrue(set.contains("value"));
        assertTrue(set.contains("anotherValue"));

    }

    @Test
    void testGetIncludeListFilterExcludes() {
        
        FilterOptions options = FilterOptions.builder().include("value,another_value").exclude("another_value").build();
        Optional<Set<String>> optional = options.getIncludeList();
        assertTrue(optional.isPresent());
        Set<String> set = optional.get();
        assertEquals(1, set.size());
        assertTrue(set.contains("value"));
        
        
    }

    @Test
    void testGetExcludeListNull() {

        FilterOptions options = FilterOptions.builder().exclude(null).build();
        assertTrue(options.getIncludeList().isEmpty());

    }

    @Test
    void testGetExcludeListNotNull() {

        FilterOptions options = FilterOptions.builder().exclude("value,another_value").build();
        Optional<Set<String>> optional = options.getExcludeList();
        assertTrue(optional.isPresent());
        Set<String> set = optional.get();
        assertEquals(2, set.size());
        assertTrue(set.contains("value"));
        assertTrue(set.contains("anotherValue"));

    }

}