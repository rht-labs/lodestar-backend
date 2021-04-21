package com.redhat.labs.lodestar.model.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import javax.ws.rs.WebApplicationException;

import org.bson.conversions.Bson;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class StateSearchComponentTest {

    static final String EXCEPTION_MESSAGE_WITH_STATE = "'state' search parameter requires 'state', 'start', and 'end' search parameter to be provided.";
    static final String EXCEPTION_MESSAGE_WITHOUT_STATE = "if either 'start', or 'end' search parameters specified, both need to be provided.";

    static final String START = LocalDate.now().toString();
    static final String END = LocalDate.now().plusDays(5).toString();

    @ParameterizedTest
    @MethodSource("provideInvalidValues")
    void testInvalidConfiguration(EngagementState state, String start, String end, String exceptionMessage) {

        StateSearchComponent ssc = StateSearchComponent.builder().state(state).start(start).end(end).build();

        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> {
            ssc.getBson();
        });

        assertEquals(400, exception.getResponse().getStatus());
        assertEquals(exceptionMessage, exception.getMessage());

    }

    @ParameterizedTest
    @MethodSource("provideStateValues")
    void testValidConfiguration(EngagementState state, String start, String end, String expected) {

        StateSearchComponent ssc = StateSearchComponent.builder().state(state).start(start).end(end).build();

        Optional<Bson> result = ssc.getBson();
        assertTrue(result.isPresent());
        assertEquals(expected, result.get().toString());

    }

    private static Stream<Arguments> provideInvalidValues() {
        return Stream.of(Arguments.of(null, START, END, EXCEPTION_MESSAGE_WITH_STATE),
                Arguments.of(EngagementState.ACTIVE, null, END, EXCEPTION_MESSAGE_WITHOUT_STATE),
                Arguments.of(EngagementState.ACTIVE, START, null, EXCEPTION_MESSAGE_WITHOUT_STATE));
    }

    private static Stream<Arguments> provideStateValues() {
        return Stream.of(Arguments.of(EngagementState.UPCOMING, START, END,
                "Or Filter{filters=[Operator Filter{fieldName='launch', operator='$exists', value=BsonBoolean{value=false}}, Filter{fieldName='launch', value=null}]}"),
                Arguments.of(EngagementState.ACTIVE, START, END,
                        "And Filter{filters=[Operator Filter{fieldName='launch', operator='$exists', value=BsonBoolean{value=true}}, Operator Filter{fieldName='endDate', operator='$gte', value="
                                + START + "}]}"),
                Arguments.of(EngagementState.PAST, START, END,
                        "And Filter{filters=[Operator Filter{fieldName='launch', operator='$exists', value=BsonBoolean{value=true}}, Operator Filter{fieldName='endDate', operator='$lt', value="
                                + START + "}]}"),
                Arguments.of(EngagementState.TERMINATING, START, END,
                        "And Filter{filters=[Operator Filter{fieldName='launch', operator='$exists', value=BsonBoolean{value=true}}, And Filter{filters=[Operator Filter{fieldName='endDate', operator='$lt', value="
                                + START + "}, Operator Filter{fieldName='archiveDate', operator='$gt', value=" + START
                                + "}]}]}"));
    }

}
