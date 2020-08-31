package com.redhat.labs.omp.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.omp.model.Category;
import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.omp.model.event.BackendEvent;
import com.redhat.labs.omp.model.event.EventType;
import com.redhat.labs.omp.repository.CategoryRepository;

import io.quarkus.vertx.ConsumeEvent;

@ApplicationScoped
public class CategoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CategoryService.class);

    @Inject
    CategoryRepository repository;

    /**
     * Returns an {@link Optional} containing a {@link Category} if the provided
     * name is found. Otherwise, an empty {@link Optional}. Will perform a case
     * insensitive search if matchCase is false.
     * 
     * @param name
     * @param matchCase
     * @return
     */
    public Optional<Category> get(String name, boolean matchCase) {
        return repository.findCategory(name, matchCase);
    }

    /**
     * Returns a {@link List} of all {@link Category} in the the data store.
     * 
     * @return
     */
    public List<Category> getAll() {
        return repository.listAll();
    }

    /**
     * Returns a {@link List} of {@link Category} names that match the partial
     * input {@link String}.
     * 
     * @param input
     * @return
     */
    public List<Category> search(String input) {
        return repository.searchCategories(input);
    }

    /**
     * Creates a {@link Category} for each category in the {@link List} if one does not
     * already exist.
     * 
     * @param categoryList
     */
    public void create(List<Category> categoryList) {
        categoryList.stream().forEach(name -> create(name));
    }

    /**
     * Creates a {@link Category} if it does not already exist.
     * 
     * @param name
     */
    public void create(Category category) {

        Optional<Category> optional = get(category.getName(), false);
        if (optional.isEmpty()) {
            LOGGER.debug("category name {} not found, persisting.", category.getName());
            repository.persist(category);
        }

    }

    /**
     * Aggregates the {@link Category} from the {@link List} of {@link Engagement}
     * and persists any new {@link Category}.
     * 
     * @param engagementList
     */
    public void createFromEngagementList(List<Engagement> engagementList) {

        LOGGER.debug("engagement list size {}", engagementList.size());

        // aggregate category lists from engagements
        List<Category> categoryList =
            engagementList.stream()
                .map(engagement -> engagement.getCategories())
                .flatMap(list -> list.stream())
                .collect(Collectors.toList());

        LOGGER.debug("creating categories from list {}", categoryList);

        // create any required categories
        create(categoryList);

    }

    /**
     * Processes {@link Category} from {@link List} of {@link Engagement}.
     * 
     * @param purgeFirst
     * @param event
     */
    void processCategoriesFromEvent(boolean purgeFirst, BackendEvent event) {

        if(null != event && null != event.getEngagementList()) {

            LOGGER.debug("purging and recreating categories from engagement list.");

            if(purgeFirst) {

                // delete all categories in data store
                repository.deleteAll();

            }

            // create from engagement list
            createFromEngagementList(event.getEngagementList());

        }

    }

    /**
     * Consumes a {@link BackendEvent} to trigger processing of any {@link Category}
     * from the {@link List} of {@link Engagement}.
     * 
     * @param event
     */
    @ConsumeEvent(EventType.Constants.INSERT_CATEGORIES_IN_DB_ADDRESS)
    void consumeCreateFromEngagementListEvent(BackendEvent event) {
        processCategoriesFromEvent(false, event);
    }

    /**
     * Consumes a {@link BackendEvent} to trigger processing of any {@link Category}
     * from the {@link List} of {@link Engagement}.  Drops existing {@link Category}
     * first.
     * 
     * @param event
     */
    @ConsumeEvent(EventType.Constants.PURGE_AND_INSERT_CATEGORIES_IN_DB_ADDRESS)
    void consumePurgeAndCreateFromEngagementListEvent(BackendEvent event) {
        processCategoriesFromEvent(true, event);
    }

}
