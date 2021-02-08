package com.redhat.labs.lodestar.validation;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

import com.redhat.labs.lodestar.utils.EmbeddedMongoTest;

import io.quarkus.test.junit.QuarkusTest;

@EmbeddedMongoTest
@QuarkusTest
class NameValidatorTest {

    @Inject
    NameValidator validator;

    @Test
    void testValueIsNull() {

        Assert.assertFalse(validator.isValid(null, null));

    }

    @Test
    void testValueIsEmptyString() {

        String input = "";
        Assert.assertFalse(validator.isValid(input, null));

    }

    @Test
    void testValueIsWhitespace() {

        String input = "   ";
        Assert.assertFalse(validator.isValid(input, null));

    }

    @Test
    void testStartsWithLetter() {

        String input = "Diw34 Id (d)._";
        Assert.assertTrue(validator.isValid(input, null));

    }

    @Test
    void testStartsWithLetterAnyLanguage() {

        String input = "øDiw34 Id (d)._";
        Assert.assertTrue(validator.isValid(input, null));

    }

    @Test
    void testStartsWithDigit() {

        String input = "23øDiw34 Id (d)._";
        Assert.assertTrue(validator.isValid(input, null));

    }

    @Test
    void testStartsWithUnderscore() {

        String input = "_Diw34 Id (d)._";
        Assert.assertTrue(validator.isValid(input, null));

    }

    @Test
    void testStartsWithLeftParenthesis() {

        String input = "(Diw34 Id (d)._";
        Assert.assertFalse(validator.isValid(input, null));

    }

    @Test
    void testStartsWithRightParenthesis() {

        String input = ")Diw34 Id (d)._";
        Assert.assertFalse(validator.isValid(input, null));

    }

    @Test
    void testStartsWithPeriod() {

        String input = ".Diw34 Id (d)._";
        Assert.assertFalse(validator.isValid(input, null));

    }

    @Test
    void testStartsWithSpace() {

        String input = " Diw34 Id (d)._";
        Assert.assertFalse(validator.isValid(input, null));

    }

    @Test
    void testStartsWithHyphen() {

        String input = "-Diw34 Id (d)._";
        Assert.assertFalse(validator.isValid(input, null));

    }

    // letters, digits, emojis, '_', '.', dash, space, parenthesis
    @Test
    void testAllValidCharacters() {

        String input = "aDi†¥øw34 Id (d)._";
        Assert.assertFalse(validator.isValid(input, null));

    }

    @Test
    void testAllInValidCharactersExclaimation() {

        String input = "aDi†¥øw!34 Id (d)._";
        Assert.assertFalse(validator.isValid(input, null));

    }

}
