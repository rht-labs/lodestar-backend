package com.redhat.labs.lodestar.util;

import java.util.List;
import java.util.Optional;

import javax.inject.Singleton;

import org.eclipse.microprofile.jwt.JsonWebToken;


@Singleton
public class JWTUtils {

    private static final String NAME_CLAIM = "name";
    private static final String PREFERRED_USERNAME_CLAIM = "preferred_username";
    private static final String USER_EMAIL_CLAIM = "email";
    
    public static final String DEFAULT_USERNAME = "lodestar-user";
    public static final String DEFAULT_EMAIL = "lodestar-email";
    
    public String getUsernameFromToken(JsonWebToken jwt) {

        // Use `name` claim first
        Optional<String> optional = claimIsValid(jwt, NAME_CLAIM);

        if (optional.isPresent()) {
            return optional.get();
        }

        // use `preferred_username` claim if `name` not valid
        optional = claimIsValid(jwt, PREFERRED_USERNAME_CLAIM);

        if (optional.isPresent()) {
            return optional.get();
        }

        // use `email` if username not valid
        return getUserEmailFromToken(jwt);

    }

    public String getUserEmailFromToken(JsonWebToken jwt) {

        Optional<String> optional = claimIsValid(jwt, USER_EMAIL_CLAIM);

        if (optional.isPresent()) {
            return optional.get();
        }

        return DEFAULT_EMAIL;

    }
    
    public boolean isAllowedToWriteEngagement(JsonWebToken jwt, List<String> allowedGroups) {
        return jwt.getGroups().stream().filter(allowedGroups::contains).findAny().isPresent();
    }

    public Optional<String> claimIsValid(JsonWebToken jwt, String claimName) {

        // get claim by name
        Optional<String> optional = jwt.claim(claimName);

        // return if no value found
        if (!optional.isPresent()) {
            return optional;
        }

        String value = optional.get();

        // return empty optional if value is whitespace
        if (value.trim().equals("")) {
            return Optional.empty();
        }

        // valid return
        return optional;

    }
}
