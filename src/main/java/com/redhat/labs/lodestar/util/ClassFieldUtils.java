package com.redhat.labs.lodestar.util;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClassFieldUtils {

    private ClassFieldUtils() {
        throw new IllegalStateException("Utility class");
    }

    /*
     * Map of known inconsistencies between the json attribute name and the class
     * field name.
     */
    private static Map<String, String> map = Map.of("engagement_region", "region", "engagement_type", "type",
            "engagement_categories", "categories", "status.overall_status", "status.status", "commits.web_url", "url",
            "engagement_categories.name", "categories.name", "engagement_categories.count", "categories.count");

    public static String snakeToCamelCase(String value) {

        if(!value.contains("_")) {
            return value;
        }

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

    public static String getFieldNameFromQueryName(String name) {

        if (map.containsKey(name)) {
            return snakeToCamelCase(map.get(name));
        }

        return snakeToCamelCase(name);

    }

}