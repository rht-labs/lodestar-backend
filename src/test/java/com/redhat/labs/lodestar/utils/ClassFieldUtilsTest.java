package com.redhat.labs.lodestar.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.redhat.labs.lodestar.util.ClassFieldUtils;

class ClassFieldUtilsTest {

    @ParameterizedTest
    @CsvSource({"alreadyCamelCase,alreadyCamelCase", "snake_case_variable,snakeCaseVariable"})
    void testSnakeToCamelCaseAlreadyCamelCase(String input, String expected) {
        assertEquals(expected, ClassFieldUtils.snakeToCamelCase(input));
    }
    
}
