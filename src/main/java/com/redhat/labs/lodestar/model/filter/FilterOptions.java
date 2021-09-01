package com.redhat.labs.lodestar.model.filter;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import com.redhat.labs.lodestar.util.ClassFieldUtils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FilterOptions {

    @Parameter(name = "include", required = false, description = "comma separated list of field names to include in response")
    @QueryParam("include")
    private String include;

    @Parameter(name = "exclude", required = false, description = "comma separated list of field names to exclude in response")
    @QueryParam("exclude")
    private String exclude;

    /**
     * Returns a {@link Set} of {@link String} from the configured include list.
     * 
     * @return
     */
    public Optional<Set<String>> getIncludeList() {

        Optional<Set<String>> includeOptional = createSet(include);

        if (includeOptional.isEmpty() || null == exclude) {
            return includeOptional;
        }

        // get exclude set
        Set<String> excludeSet = getExcludeList().orElse(Set.of());

        // filter any includes also in exclude
        return Optional.of(includeOptional.get().stream().filter(attribute -> !excludeSet.contains(attribute))
                .collect(Collectors.toSet()));

    }

    /**
     * Creates an {@link Optional} containing a {@link Set} of {@link String} for
     * the values to exclude.
     * 
     * @return
     */
    public Optional<Set<String>> getExcludeList() {
        return createSet(exclude);
    }

    /**
     * Returns an {@link Optional} of a {@link Set} of {@link String} after parsing
     * the given value.
     * 
     * @param value
     * @return
     */
    private Optional<Set<String>> createSet(String value) {

        if (null == value) {
            return Optional.empty();
        }

        return Optional.of(parseAttributes(value));

    }

    /**
     * Parses the given value to get the attributes in a {@link Set}.
     * 
     * @param value
     * @return
     */
    private Set<String> parseAttributes(String value) {

        if (null == value || value.isEmpty()) {
            return new HashSet<>();
        }

        return Stream.of(value.split(",")).map(ClassFieldUtils::getFieldNameFromQueryName).collect(Collectors.toSet());

    }

}