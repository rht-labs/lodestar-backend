package com.redhat.labs.lodestar.model.search;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.or;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bson.conversions.Bson;

import lombok.Builder;

@Builder
public class BsonSearch {

    private static final String STATE = "state";
    private static final String START = "start";
    private static final String END = "end";
    private static final String SEARCH_DELIMITER = "&";
    private static final String EQUALS = "=";

    private String searchString;

    /**
     * Creates the {@link Bson} based on the given search {@link String}.
     * 
     * @param search
     * @return
     */
    public Optional<Bson> createBsonForSearch() {

        // parse the search string
        Optional<Map<Object, Map<Object, List<DefaultSearchComponent>>>> searchComponentMap = parseSearchString();
        if (searchComponentMap.isEmpty()) {
            return Optional.empty();
        }

        // create state search
        Optional<Bson> stateSearchBson = createStateSearchComponent(searchComponentMap.get());

        // create bson for all other attributes
        Optional<Bson> searchBson = convertToBson(searchComponentMap.get());

        if (searchBson.isPresent() && stateSearchBson.isPresent()) {
            return Optional.of(and(stateSearchBson.get(), searchBson.get()));
        } else if (searchBson.isPresent()) {
            return Optional.of(searchBson.get());
        } else if (stateSearchBson.isPresent()) {
            return Optional.of(stateSearchBson.get());
        } else {
            return Optional.empty();
        }

    }

    /**
     * Creates a {@link Bson} for search using state and a date range.
     * 
     * @param searchComponentMap
     * @return
     */
    private Optional<Bson> createStateSearchComponent(
            Map<Object, Map<Object, List<DefaultSearchComponent>>> searchComponentMap) {

        // create state search if required
        if (searchComponentMap.containsKey(STATE)) {
            // get state value
            EngagementState state = EngagementState
                    .lookup(findFirstSearchComponentValue(searchComponentMap, STATE, EQUALS).orElse(null));
            searchComponentMap.remove(STATE);

            // get start value
            String start = findFirstSearchComponentValue(searchComponentMap, START, EQUALS).orElse(null);
            searchComponentMap.remove(START);

            // get end value
            String end = findFirstSearchComponentValue(searchComponentMap, END, EQUALS).orElse(null);
            searchComponentMap.remove(END);

            // build state searchc component
            StateSearchComponent ssc = StateSearchComponent.builder().state(state).start(start).end(end).build();
            return ssc.getBson();

        }

        return Optional.empty();

    }

    /**
     * Parses the given search string to return a {@link List} of
     * {@link DefaultSearchComponent} grouping by attribute name and then operator.
     * 
     * @param search
     * @return
     */
    private Optional<Map<Object, Map<Object, List<DefaultSearchComponent>>>> parseSearchString() {

        if (null == searchString) {
            return Optional.empty();
        }

        String[] components = searchString.split(SEARCH_DELIMITER);

        // group by attribute name and then operator
        return Optional.of(Stream.of(components).map(c -> DefaultSearchComponent.builder().component(c).build())
                .filter(sc -> sc.getAttribute().isPresent() && sc.getOperator().isPresent())
                .collect(Collectors.groupingBy(sc -> sc.getAttribute().get(),
                        Collectors.groupingBy(sc -> sc.getOperator().get()))));

    }

    /**
     * Converts the {@link Map} containing {@link DefaultSearchComponent}s to a
     * {@link Bson} representing the search criteria.
     * 
     * @param componentMap
     * @return
     */
    private Optional<Bson> convertToBson(Map<Object, Map<Object, List<DefaultSearchComponent>>> searchComponentMap) {

        // field component
        List<Bson> andBsonList = searchComponentMap.entrySet().stream().map(fieldEntry -> {

            // and the bson for each operator in its list
            List<Bson> bsonList = fieldEntry.getValue().entrySet().stream().map(Entry::getValue).flatMap(List::stream)
                    .map(DefaultSearchComponent::getBson).filter(Optional::isPresent).map(Optional::get)
                    .collect(Collectors.toList());

            return 1 == bsonList.size() ? bsonList.get(0) : or(bsonList);

        }).collect(Collectors.toList());

        return andBsonList.isEmpty() ? Optional.empty() : Optional.of(and(andBsonList));

    }

    /**
     * Returns an {@link Optional} containing the value of {@link DefaultSearchComponent}.
     * Otherwise, returns an empty {@link Optional}.
     * 
     * @param searchComponent
     * @return
     */
    private Optional<String> getValueFromSearchComponent(Optional<DefaultSearchComponent> searchComponent) {

        return searchComponent.filter(c -> c.getValue().isPresent()).map(DefaultSearchComponent::getValue)
                .filter(Optional::isPresent).map(Optional::get);
    }

    /**
     * Returns an {@link Optional} containing the value associated to the
     * {@link DefaultSearchComponent} for the given attribute name and operator names.
     * Otherwise, returns an empty {@link Optional}.
     * 
     * @param searchComponentMap
     * @param attributeName
     * @param operator
     * @return
     */
    private Optional<String> findFirstSearchComponentValue(
            Map<Object, Map<Object, List<DefaultSearchComponent>>> searchComponentMap, String attributeName, String operator) {

        Optional<DefaultSearchComponent> searchComponent = findFirtSearchComponent(searchComponentMap, attributeName,
                operator);
        return getValueFromSearchComponent(searchComponent);

    }

    /**
     * Returns an {@link Optional} containing the first {@link DefaultSearchComponent} in
     * the {@link Map} for the given attribute name and operator name. Otherwise,
     * returns an empty {@link Optional}.
     * 
     * @param searchComponentMap
     * @param attribute
     * @param operator
     * @return
     */
    private Optional<DefaultSearchComponent> findFirtSearchComponent(
            Map<Object, Map<Object, List<DefaultSearchComponent>>> searchComponentMap, String attribute, String operator) {

        return findSearchComponentList(searchComponentMap, attribute, operator).stream().findFirst();

    }

    /**
     * Returns a {@link List} of {@link DefaultSearchComponent} in the {@link Map} for the
     * given attribute name and operator name. Otherwise, an empty {@link List} is
     * returned.
     * 
     * @param searchComponentMap
     * @param attribute
     * @param operator
     * @return
     */
    private List<DefaultSearchComponent> findSearchComponentList(
            Map<Object, Map<Object, List<DefaultSearchComponent>>> searchComponentMap, String attribute, String operator) {

        // get map for given attribute
        Map<Object, List<DefaultSearchComponent>> operatorMap = searchComponentMap.get(attribute);
        if (null == operatorMap) {
            return Arrays.asList();
        }

        return operatorMap.entrySet().stream().filter(e -> e.getKey().equals(operator))
                .filter(e -> null != e.getValue()).flatMap(e -> e.getValue().stream()).collect(Collectors.toList());

    }

}
