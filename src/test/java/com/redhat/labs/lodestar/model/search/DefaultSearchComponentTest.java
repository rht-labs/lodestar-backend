package com.redhat.labs.lodestar.model.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DefaultSearchComponentTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "  ", "customer_name-that" })
    void testInvalidComponent(String component) {

        DefaultSearchComponent sc = DefaultSearchComponent.builder().component(component).build();

        assertTrue(sc.getAttribute().isEmpty());
        assertTrue(sc.getOperator().isEmpty());
        assertTrue(sc.getValue().isEmpty());

        assertTrue(sc.getBson().isEmpty());

    }

    @ParameterizedTest
    @MethodSource("provideOperatorTestValues")
    void testOperators(String component, String attribute, String operator, String value, String bson) {

        DefaultSearchComponent sc = DefaultSearchComponent.builder().component(component).build();

        assertTrue(sc.getAttribute().isPresent());
        assertEquals(attribute, sc.getAttribute().get());

        assertTrue(sc.getOperator().isPresent());
        assertEquals(operator, sc.getOperator().get());

        if (!operator.contains("exists")) {
            assertTrue(sc.getValue().isPresent());
            assertEquals(value, sc.getValue().get());
        }

        assertTrue(sc.getBson().isPresent());
        assertEquals(bson, sc.getBson().get().toString());

    }

    private static Stream<Arguments> provideOperatorTestValues() {
        return Stream.of(
                Arguments.of("customer_name=that", "customer_name", "=", "that",
                        "Filter{fieldName='customerName', value=that}"),
                Arguments.of("customer_name!=that", "customer_name", "!=", "that",
                        "Not Filter{filter=Filter{fieldName='customerName', value=that}}"),
                Arguments.of("customer_name like that", "customer_name", "like", "that",
                        "Operator Filter{fieldName='customerName', operator='$eq', value=BsonRegularExpression{pattern='that', options='i'}}"),
                Arguments.of("customer_name not like that", "customer_name", "not like", "that",
                        "Not Filter{filter=Operator Filter{fieldName='customerName', operator='$eq', value=BsonRegularExpression{pattern='that', options='i'}}}"),
                Arguments.of("customer_name exists", "customer_name", "exists", "",
                        "And Filter{filters=[Operator Filter{fieldName='customerName', operator='$exists', value=BsonBoolean{value=true}}, Operator Filter{fieldName='customerName', operator='$ne', value=null}, Operator Filter{fieldName='customerName', operator='$ne', value=[]}]}"),
                Arguments.of("customer_name not exists", "customer_name", "not exists", "",
                        "Or Filter{filters=[And Filter{filters=[Operator Filter{fieldName='customerName', operator='$exists', value=BsonBoolean{value=false}}, Filter{fieldName='customerName', value=null}]}, And Filter{filters=[Operator Filter{fieldName='customerName', operator='$exists', value=BsonBoolean{value=true}}, Filter{fieldName='customerName', value=[]}]}]}"),
                Arguments.of("customer_name=one,two,three", "customer_name", "=", "one,two,three",
                        "Or Filter{filters=[Filter{fieldName='customerName', value=one}, Filter{fieldName='customerName', value=two}, Filter{fieldName='customerName', value=three}]}"));
    }

}
