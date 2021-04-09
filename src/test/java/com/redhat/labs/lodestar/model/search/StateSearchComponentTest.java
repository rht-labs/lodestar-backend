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

    static final String EXCEPTION_MESSAGE = "'state' search parameter requires 'state', 'start', and 'end' search parameter to be provided.";
    static final String START = LocalDate.now().toString();
    static final String END = LocalDate.now().plusDays(5).toString();

    @ParameterizedTest
    @MethodSource("provideInvalidValues")
    void testInvalidConfiguration(EngagementState state, String start, String end) {

        StateSearchComponent ssc = StateSearchComponent.builder().state(state).start(start).end(end).build();

        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> {
            ssc.getBson();
        });

        assertEquals(400, exception.getResponse().getStatus());
        assertEquals(EXCEPTION_MESSAGE, exception.getMessage());

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
        return Stream.of(Arguments.of(null, START, END), Arguments.of(EngagementState.ACTIVE, null, END),
                Arguments.of(EngagementState.ACTIVE, START, null));
    }

    private static Stream<Arguments> provideStateValues() {
        return Stream.of(Arguments.of(EngagementState.UPCOMING, START, END,
                "And Filter{filters=[Operator Filter{fieldName='launch', operator='$exists', value=BsonBoolean{value=false}}, And Filter{filters=[Operator Filter{fieldName='startDate', operator='$gte', value="
                        + START + "}, Operator Filter{fieldName='startDate', operator='$lte', value=" + END + "}]}]}"),
                Arguments.of(EngagementState.ACTIVE, START, END,
                        "And Filter{filters=[Operator Filter{fieldName='launch', operator='$exists', value=BsonBoolean{value=true}}, Operator Filter{fieldName='endDate', operator='$gte', value="
                                + END + "}]}"),
                Arguments.of(EngagementState.PAST, START, END,
                        "And Filter{filters=[Operator Filter{fieldName='launch', operator='$exists', value=BsonBoolean{value=true}}, Operator Filter{fieldName='endDate', operator='$lt', value="
                                + END + "}]}"),
                Arguments.of(EngagementState.TERMINATING, START, END,
                        "And Filter{filters=[Operator Filter{fieldName='launch', operator='$exists', value=BsonBoolean{value=true}}, And Filter{filters=[Operator Filter{fieldName='endDate', operator='$lt', value="
                                + END + "}, Operator Filter{fieldName='archiveDate', operator='$gt', value=" + END
                                + "}]}]}"));
    }

}
