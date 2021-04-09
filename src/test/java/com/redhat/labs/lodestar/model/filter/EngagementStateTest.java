package com.redhat.labs.lodestar.model.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.redhat.labs.lodestar.model.search.EngagementState;

class EngagementStateTest {

    @ParameterizedTest
    @MethodSource("provideValues")
    void testLookup(String input, EngagementState expected) {
        assertEquals(expected, EngagementState.lookup(input));
    }

    private static Stream<Arguments> provideValues() {
        return Stream.of(
                Arguments.of("Active", EngagementState.ACTIVE), Arguments.of("UpcoMing", EngagementState.UPCOMING),
                Arguments.of("PasT", EngagementState.PAST), Arguments.of("TeRminAting", EngagementState.TERMINATING),
                Arguments.of("unknown", null));
    }

}
