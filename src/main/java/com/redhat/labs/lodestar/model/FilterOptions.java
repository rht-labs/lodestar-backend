package com.redhat.labs.lodestar.model;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterOptions {

    @Setter
    private String include;
    @Setter
    private String exclude;

    public Optional<Set<String>> getIncludeList() {

        Optional<Set<String>> includeOptional = createSet(include);

        if (includeOptional.isEmpty() || null == exclude) {
            return includeOptional;
        }

        // get exlude set
        Set<String> excludeSet = getExcludeList().orElse(Set.of());

        // filter any includes also in exclude
        return Optional.of(includeOptional.get().stream().filter(attribute -> !excludeSet.contains(attribute))
                .collect(Collectors.toSet()));

    }

    public Optional<Set<String>> getExcludeList() {
        return createSet(exclude);
    }

    private Optional<Set<String>> createSet(String value) {

        if (null == value) {
            return Optional.empty();
        }

        return Optional.of(parseAttributes(value));

    }

    private Set<String> parseAttributes(String value) {

        if (null == value || value.isEmpty()) {
            return new HashSet<>();
        }

        return Stream.of(value.split(",")).map(this::snakeToCamelCase).collect(Collectors.toSet());

    }

    private String snakeToCamelCase(String value) {

        // split lowercase value based on underscore
        List<String> tokens = Stream.of(value.toLowerCase().split("_")).collect(Collectors.toList());

        // start string with first lower case token
        StringBuilder builder = new StringBuilder(tokens.remove(0));

        // capitalize first letter of each remaining token
        tokens.stream().forEach(token -> {
            String tmp = (1 == token.length()) ? token.toUpperCase()
                    : token.substring(0, 1).toUpperCase() + token.substring(1);
            builder.append(tmp);
        });

        return builder.toString();

    }

}
