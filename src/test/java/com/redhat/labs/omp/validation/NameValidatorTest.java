package com.redhat.labs.omp.validation;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

import com.redhat.labs.utils.EmbeddedMongoTest;

import io.quarkus.test.junit.QuarkusTest;

@EmbeddedMongoTest
@QuarkusTest
public class NameValidatorTest {

    @Inject
    NameValidator validator;

    @Test
    public void testValueIsNull() {

        Assert.assertFalse(validator.isValid(null, null));

    }

    @Test
    public void testValueIsEmptyString() {

        String input = "";
        Assert.assertFalse(validator.isValid(input, null));

    }

    @Test
    public void testValueIsWhitespace() {

        String input = "   ";
        Assert.assertFalse(validator.isValid(input, null));

    }

    @Test
    public void testStartsWithLetter() {

        String input = "Diw34 Id (d)._";
        Assert.assertTrue(validator.isValid(input, null));

    }

    @Test
    public void testStartsWithLetterAnyLanguage() {

        String input = "øDiw34 Id (d)._";
        Assert.assertTrue(validator.isValid(input, null));

    }

    @Test
    public void testStartsWithDigit() {

        String input = "23øDiw34 Id (d)._";
        Assert.assertTrue(validator.isValid(input, null));

    }

    @Test
    public void testStartsWithUnderscore() {

        String input = "_Diw34 Id (d)._";
        Assert.assertTrue(validator.isValid(input, null));

    }

    @Test
    public void testStartsWithLeftParenthesis() {

        String input = "(Diw34 Id (d)._";
        Assert.assertFalse(validator.isValid(input, null));

    }

    @Test
    public void testStartsWithRightParenthesis() {

        String input = ")Diw34 Id (d)._";
        Assert.assertFalse(validator.isValid(input, null));

    }

    @Test
    public void testStartsWithPeriod() {

        String input = ".Diw34 Id (d)._";
        Assert.assertFalse(validator.isValid(input, null));

    }

    @Test
    public void testStartsWithSpace() {

        String input = " Diw34 Id (d)._";
        Assert.assertFalse(validator.isValid(input, null));

    }

    @Test
    public void testStartsWithHyphen() {

        String input = "-Diw34 Id (d)._";
        Assert.assertFalse(validator.isValid(input, null));

    }

    // letters, digits, emojis, '_', '.', dash, space, parenthesis
    @Test
    public void testAllValidCharacters() {

        String input = "aDi†¥øw34 Id (d)._";
        Assert.assertFalse(validator.isValid(input, null));

    }

    @Test
    public void testAllInValidCharactersExclaimation() {

        String input = "aDi†¥øw!34 Id (d)._";
        Assert.assertFalse(validator.isValid(input, null));

    }

}
