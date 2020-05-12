package com.redhat.labs.omp.validation;

import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class NameValidator implements ConstraintValidator<ValidName, String> {

    // Defaults to GitLab Group Name Pattern:
    // GitLab currently has the following restrictions on group names:
    // Names can contain only letters, digits, emojis, '_', '.', dash, space,
    // parenthesis. It must start with letter, digit, emoji or '_'.
    @ConfigProperty(name = "valid.name.regex", defaultValue = "^(\\p{L}|\\p{N}|_)[\\p{L}\\p{N}\\p{Z}-_\\.\\(\\)]*$")
    String validNameRegex;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        Pattern pattern = Pattern.compile(validNameRegex, Pattern.UNICODE_CHARACTER_CLASS);
        return pattern.matcher(value).matches();

    }

}
