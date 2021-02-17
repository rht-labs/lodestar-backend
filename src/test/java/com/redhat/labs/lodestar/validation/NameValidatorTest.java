package com.redhat.labs.lodestar.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.wildfly.common.Assert;

class NameValidatorTest {

    NameValidator validator;
    
    @BeforeEach
    void setup() {
        validator = new NameValidator();
        validator.validNameRegex = "^(\\p{L}|\\p{N}|_)[\\p{L}\\p{N}\\p{Z}-_\\.\\(\\)]*$";
    }

    private static String[] invalidValues() {
        return new String[] { null, "", "   ", "(Diw34 Id (d)._", ")Diw34 Id (d)._", ".Diw34 Id (d)._",
                " Diw34 Id (d)._", "-Diw34 Id (d)._", "aDi†¥øw34 Id (d)._", "aDi†¥øw!34 Id (d)._" };
    }

    private static String[] validValues() {
        return new String[] { "Diw34 Id (d)._", "øDiw34 Id (d)._", "23øDiw34 Id (d)._", "_Diw34 Id (d)._" };
    }

    @ParameterizedTest
    @MethodSource("invalidValues")
    void testInvalidScenarios(String input) {
        Assert.assertFalse(validator.isValid(input, null));
    }

    @ParameterizedTest
    @MethodSource("validValues")
    void testValidScenarios(String input) {
        Assert.assertTrue(validator.isValid(input, null));
    }

}