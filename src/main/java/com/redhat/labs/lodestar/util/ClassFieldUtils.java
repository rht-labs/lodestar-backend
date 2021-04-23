package com.redhat.labs.lodestar.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang3.reflect.FieldUtils;

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

        if (!value.contains("_")) {
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

    public static String classFieldNamesAsCommaSeparatedString(Class<?> clazz, Optional<String> prefix) {

        Field[] fields = FieldUtils.getAllFields(clazz);
        List<String> names = Stream.of(fields).map(f -> ClassFieldUtils.getNestedFieldName(f.getName(), prefix))
                .collect(Collectors.toList());

        return String.join(",", names);

    }

    public static String getFieldNameFromQueryName(String name) {

        if (map.containsKey(name)) {
            return snakeToCamelCase(map.get(name));
        }

        return snakeToCamelCase(name);

    }

    /**
     * Determines the type of the given field name and then creates an instance of
     * that type for the provided value.
     * 
     * @param fieldName
     * @param value
     * @return
     */
    public static Object getObjectFromString(Class<?> parentClazz, String fieldName, String value) {

        Class<?> clazz = getTypeFromFieldName(parentClazz, fieldName);
        if (null == clazz) {
            throw new WebApplicationException("could not get class for engagement.", 500);
        }
        try {
            Constructor<?> constructor = clazz.getConstructor(String.class);
            return constructor.newInstance(value);
        } catch (Exception e) {
            throw new WebApplicationException(
                    "cannot create instance of " + clazz.getName() + ", error = " + e.getMessage(), 400);
        }
    }

    /**
     * Returns the type associated to the given fieldName.
     * 
     * @param clazz
     * @param fieldName
     * @return
     */
    static Class<?> getTypeFromFieldName(Class<?> clazz, String fieldName) {

        if (null == fieldName) {
            return null;
        }

        String current = fieldName;
        String nested = null;

        if (current.contains(".")) {
            current = fieldName.substring(0, fieldName.indexOf("."));
            nested = fieldName.substring(fieldName.indexOf(".") + 1);
        }

        try {

            Field f = clazz.getDeclaredField(current);
            Class<?> fieldClass = f.getType();

            if (nested == null) {
                return fieldClass;
            }

            Class<?> nextClass = f.getType();
            Type type = f.getGenericType();
            if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type;
                if (pt.getActualTypeArguments().length == 1) {
                    Type t = pt.getActualTypeArguments()[0];
                    nextClass = Class.forName(t.getTypeName());
                }
            }
            return getTypeFromFieldName(nextClass, nested);

        } catch (Exception e) {
            throw new WebApplicationException(String.format("invalid field %s on %s", fieldName, clazz.getName()), 400);
        }

    }

    static String getNestedFieldName(String fieldName, Optional<String> prefix) {
        return prefix.isPresent() ? new StringBuilder(prefix.get()).append(".").append(fieldName).toString()
                : fieldName;
    }

}