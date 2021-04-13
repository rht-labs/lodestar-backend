package com.redhat.labs.lodestar.model.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Optional;

import javax.ws.rs.WebApplicationException;

import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;

class RangeSearchComponentTest {

    static final String EXCEPTION_MESSAGE = "if either 'start', or 'end' search parameters specified, both need to be provided.";

    static final String START = LocalDate.now().toString();
    static final String END = LocalDate.now().plusDays(5).toString();

    @Test
    void testGetBson() {

        RangeSearchComponent rsc = RangeSearchComponent.builder().start(START).end(END).build();
        Optional<Bson> bson = rsc.getBson();

        assertTrue(bson.isPresent());
        assertEquals(
                "And Filter{filters=[Operator Filter{fieldName='launch', operator='$exists', value=BsonBoolean{value=true}}, Or Filter{filters=[And Filter{filters=[Operator Filter{fieldName='launch.launchedDateTime', operator='$gte', value="
                        + START + "}, Operator Filter{fieldName='launch.launchedDateTime', operator='$lte', value="
                        + END + "}]}, And Filter{filters=[Operator Filter{fieldName='endDate', operator='$gte', value="
                        + START + "}, Operator Filter{fieldName='endDate', operator='$lte', value=" + END + "}]}]}]}",
                bson.get().toString());

    }

    @Test
    void testInvalidNoStart() {

        RangeSearchComponent rsc = RangeSearchComponent.builder().start(null).end(END).build();

        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> {
            rsc.getBson();
        });

        assertEquals(400, exception.getResponse().getStatus());
        assertEquals(EXCEPTION_MESSAGE, exception.getMessage());

    }

    @Test
    void testInvalidNoEnd() {

        RangeSearchComponent rsc = RangeSearchComponent.builder().start(START).end(null).build();

        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> {
            rsc.getBson();
        });

        assertEquals(400, exception.getResponse().getStatus());
        assertEquals(EXCEPTION_MESSAGE, exception.getMessage());

    }

}
