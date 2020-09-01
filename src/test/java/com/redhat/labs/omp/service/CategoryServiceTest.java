package com.redhat.labs.omp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.labs.omp.model.Category;
import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.omp.model.event.BackendEvent;
import com.redhat.labs.utils.EmbeddedMongoTest;

import io.quarkus.test.junit.QuarkusTest;
import io.vertx.mutiny.core.eventbus.EventBus;

@EmbeddedMongoTest
@QuarkusTest
public class CategoryServiceTest {

    @Inject
    CategoryService service;

    @Inject
    EventBus eventBus;

    @Test
    void testCategoryService() throws InterruptedException {

        // place list of engagements with categories on bus
        BackendEvent event = mockBackendEvent();
        eventBus.sendAndForget(event.getEventType().getEventBusAddress(), event);

        TimeUnit.SECONDS.sleep(1);

        // get all categories
        List<Category> categoryList = service.getAll();

        assertNotNull(categoryList);
        assertEquals(4, categoryList.size());

        // search
        List<Category> searchResults = service.search("e");

        assertNotNull(searchResults);
        assertEquals(1, searchResults.size());

        searchResults = service.search("c");

        assertNotNull(searchResults);
        assertEquals(3, searchResults.size());

        Optional<Category> optional = service.get("C2", true);
        assertTrue(optional.isEmpty());

        optional = service.get("C2", false);
        assertTrue(optional.isPresent());

    }

    private BackendEvent mockBackendEvent() {

        Category c1 = mockCategory("c1");
        Category c2 = mockCategory("c2");

        Engagement e1 = mockEngagement("customer1", "customer2", Arrays.asList(c1, c2));

        Category c3 = mockCategory("C2");
        Category c4 = mockCategory("c4");
        Category c5 = mockCategory("e5");

        Engagement e2 = mockEngagement("customer2", "project2", Arrays.asList(c3,c4,c5));

        return BackendEvent.createInsertCategoriesInDbRequestedEvent(Arrays.asList(e1, e2));

    }

    private Category mockCategory(String name) {
        return Category.builder().name(name).build();
    }

    private Engagement mockEngagement(String customerName, String projectName, List<Category> categories) {
        return Engagement.builder().customerName(customerName).projectName(projectName).categories(categories).build();
    }

}
