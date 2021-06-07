package com.redhat.labs.lodestar.model.search;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.ne;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.not;
import static com.mongodb.client.model.Filters.or;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.regex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bson.conversions.Bson;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.util.ClassFieldUtils;

import lombok.Builder;
import lombok.ToString;

@ToString
public class DefaultSearchComponent implements BsonSearchComponent {

    private static final String EQUALS = "=";
    private static final String NOT_EQUALS = new StringBuilder("!").append(EQUALS).toString();
    private static final String LIKE = "like";
    private static final String NOT_LIKE = new StringBuilder("not ").append(LIKE).toString();
    private static final String EXISTS = "exists";
    private static final String NOT_EXISTS = new StringBuilder("not ").append(EXISTS).toString();
    private static final String VALUE_DELIMITER = ",";

    private String component;
    private Optional<String> attribute = Optional.empty();
    private Optional<String> operator = Optional.empty();
    private Optional<String> value = Optional.empty();

    @Builder
    public DefaultSearchComponent(String component) {
        this.component = component;
    }

    public Optional<String> getAttribute() {
        if (attribute.isEmpty()) {
            parseComponent();
        }
        return attribute;
    }

    public Optional<String> getOperator() {
        if (operator.isEmpty()) {
            parseComponent();
        }
        return operator;
    }

    public Optional<String> getValue() {
        if (value.isEmpty()) {
            parseComponent();
        }
        return value;
    }

    /**
     * Creates a {@link Bson} corresponding to the given
     * {@link DefaultSearchComponent}.
     * 
     * @param searchComponent
     * @return
     */
    public Optional<Bson> getBson() {

        // return is attribute is missing or if operator is equals/like and value is
        // missing
        if (attribute.isEmpty() || (!component.contains(EXISTS) && value.isEmpty())) {
            return Optional.empty();
        }

        if (component.contains(EXISTS)) {

            return Optional.ofNullable(getBsonByOperator(null).orElse(null));

        } else {

            // split values if required
            List<Bson> bsonList = Arrays.asList(value.get().split(VALUE_DELIMITER)).stream()
                    .map(this::getBsonByOperator).filter(Optional::isPresent).map(Optional::get)
                    .collect(Collectors.toList());

            if (bsonList.size() > 1) {
                return Optional.of(or(bsonList));
            } else if (bsonList.size() == 1) {
                return Optional.of(bsonList.get(0));
            } else {
                return Optional.empty();
            }

        }

    }

    /**
     * Returns a {@link Bson} generated from the configured attribute and operator.
     * 
     * @param attributeValue
     * @return
     */
    private Optional<Bson> getBsonByOperator(String attributeValue) {

        if (attribute.isEmpty()) {
            return Optional.empty();
        }

        // convert to class attribute name
        String attributeName = ClassFieldUtils.getFieldNameFromQueryName(attribute.get());

        if (isEqualsOperator()) {
            return Optional.of(eq(attributeName,
                    ClassFieldUtils.getObjectFromString(Engagement.class, attributeName, attributeValue)));
        } else if (isNotEqualsOperator()) {
            return Optional.of(not(eq(attributeName,
                    ClassFieldUtils.getObjectFromString(Engagement.class, attributeName, attributeValue))));
        } else if (isLikeOperator()) {
            return Optional.of(regex(attributeName, attributeValue, "i"));
        } else if (isNotLikeOperator()) {
            return Optional.of(not(regex(attributeName, attributeValue, "i")));
        } else if (isExistsOperator()) { 
            Bson existsAndNotNull = and(exists(attributeName, true), ne(attributeName, null), ne(attributeName, new ArrayList<>()));            
            return Optional.of(existsAndNotNull);
        } else if (isNotExistsOperator()) {
            Bson doesNotExistAndIsNull = and(exists(attributeName, false), eq(attributeName, null));
            Bson doesExistAndEmpty = and(exists(attributeName, true), eq(attributeName, new ArrayList<>()));
            Bson notExists = or(doesNotExistAndIsNull, doesExistAndEmpty);
            return Optional.of(notExists);
        } else {
            return Optional.empty();
        }

    }

    private boolean isExistsOperator() {
        return EXISTS.equals(operator.isPresent() ? operator.get() : null);
    }

    private boolean isNotExistsOperator() {
        return NOT_EXISTS.equals(operator.isPresent() ? operator.get() : null);
    }

    private boolean isEqualsOperator() {
        return EQUALS.equals(operator.isPresent() ? operator.get() : null);
    }

    private boolean isNotEqualsOperator() {
        return NOT_EQUALS.equals(operator.isPresent() ? operator.get() : null);
    }

    private boolean isLikeOperator() {
        return LIKE.equals(operator.isPresent() ? operator.get() : null);
    }

    private boolean isNotLikeOperator() {
        return NOT_LIKE.equals(operator.isPresent() ? operator.get() : null);
    }

    private void parseComponent() {

        if (null == component) {
            return;
        }

        if (component.contains(NOT_EQUALS)) {
            operator = Optional.of(NOT_EQUALS);
        } else if (component.contains(EQUALS)) {
            operator = Optional.of(EQUALS);
        } else if (component.contains(NOT_EXISTS)) {
            operator = Optional.of(NOT_EXISTS);
        } else if (component.contains(EXISTS)) {
            operator = Optional.of(EXISTS);
        } else if (component.contains(NOT_LIKE)) {
            operator = Optional.of(NOT_LIKE);
        } else if (component.contains(LIKE)) {
            operator = Optional.of(LIKE);
        }

        if (operator.isPresent()) {
            setVariableAndValue();
        }

    }

    private void setVariableAndValue() {

        String splitString = new StringBuilder("\\s*").append(operator.get()).append("\\s*").toString();
        String[] split = component.split(splitString);

        // set variable
        attribute = Optional.of(split[0]);

        // set value if not an exists operator
        if (!(isExistsOperator() || isNotExistsOperator())) {
            value = Optional.of(split[1]);
        }

    }

}
