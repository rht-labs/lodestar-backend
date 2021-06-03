package com.redhat.labs.lodestar.model.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.stream.Stream;

import javax.ws.rs.WebApplicationException;

import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BsonSearchTest {

    static final String EXCEPTION_MESSAGE_WITH_STATE = "'state' search parameter requires 'state', 'start', and 'end' search parameter to be provided.";
    static final String EXCEPTION_MESSAGE_WITHOUT_STATE = "if either 'start', or 'end' search parameters specified, both need to be provided.";

    @Test
    void testSearchStringNull() {

        BsonSearch search = BsonSearch.builder().build();
        Optional<Bson> bson = search.createBsonForSearch();
        assertTrue(bson.isEmpty());

    }

    @ParameterizedTest
    @MethodSource("provideExceptionValues")
    void testSearchStringStateExceptions(String searchString, String exceptionMessage) {

        BsonSearch search = BsonSearch.builder().searchString(searchString).build();

        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> {
            search.createBsonForSearch();
        });

        assertEquals(400, exception.getResponse().getStatus());
        assertEquals(exceptionMessage, exception.getMessage());

    }

    @ParameterizedTest
    @MethodSource("provideSearchStringAndExpectedValues")
    void testSearchStringNoStateOrDateRange(String searchString, String expected) {

        BsonSearch search = BsonSearch.builder().searchString(searchString).build();
        Optional<Bson> bson = search.createBsonForSearch();
        assertTrue(bson.isPresent());
        assertEquals(expected, bson.get().toString());

    }

    private static Stream<Arguments> provideExceptionValues() {
        return Stream.of(Arguments.of("state=unknown&end=2020-01-01", EXCEPTION_MESSAGE_WITH_STATE),
                Arguments.of("state=active&end=2020-01-01", EXCEPTION_MESSAGE_WITHOUT_STATE),
                Arguments.of("state=active&start=2020-01-01", EXCEPTION_MESSAGE_WITHOUT_STATE));
    }

    private static Stream<Arguments> provideSearchStringAndExpectedValues() {
        return Stream.of(Arguments.of(
                "customer_name=a&project_name!=2&description like 4&engagement_lead_name not like 5&launch exists&engagement_users not exists&region=9,8,7",
                "And Filter{filters=[Not Filter{filter=Operator Filter{fieldName='engagementLeadName', operator='$eq', value=BsonRegularExpression{pattern='5', options='i'}}}, Operator Filter{fieldName='description', operator='$eq', value=BsonRegularExpression{pattern='4', options='i'}}, And Filter{filters=[Operator Filter{fieldName='launch', operator='$exists', value=BsonBoolean{value=true}}, Operator Filter{fieldName='launch', operator='$ne', value=null}, Operator Filter{fieldName='launch', operator='$ne', value=[]}]}, Filter{fieldName='customerName', value=a}, Or Filter{filters=[Filter{fieldName='region', value=9}, Filter{fieldName='region', value=8}, Filter{fieldName='region', value=7}]}, Not Filter{filter=Filter{fieldName='projectName', value=2}}, Or Filter{filters=[And Filter{filters=[Operator Filter{fieldName='engagementUsers', operator='$exists', value=BsonBoolean{value=false}}, Filter{fieldName='engagementUsers', value=null}]}, And Filter{filters=[Operator Filter{fieldName='engagementUsers', operator='$exists', value=BsonBoolean{value=true}}, Filter{fieldName='engagementUsers', value=[]}]}]}]}"),
                Arguments.of("state=active&start=2020-01-01&end=2021-01-01",
                        "And Filter{filters=[Operator Filter{fieldName='launch', operator='$exists', value=BsonBoolean{value=true}}, Operator Filter{fieldName='endDate', operator='$gte', value=2020-01-01}]}"),
                Arguments.of("customer_name=c1&state=active&start=2020-01-01&end=2021-01-01",
                        "And Filter{filters=[And Filter{filters=[Operator Filter{fieldName='launch', operator='$exists', value=BsonBoolean{value=true}}, Operator Filter{fieldName='endDate', operator='$gte', value=2020-01-01}]}, And Filter{filters=[Filter{fieldName='customerName', value=c1}]}]}"));
    }

}
