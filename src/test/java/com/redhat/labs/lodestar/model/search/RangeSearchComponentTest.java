package com.redhat.labs.lodestar.model.search;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RangeSearchComponentTest {

    @Test
    void testGetBson() {
        assertTrue(RangeSearchComponent.builder().build().getBson().isEmpty());
    }

}
