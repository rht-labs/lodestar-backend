package com.redhat.labs.omp.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.omp.model.Category;
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
    Optional<Category> get(String name, boolean matchCase) {
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
     * Consumes a {@link BackendEvent} to trigger the purge of all {@link Category}s
     * and then processing the {@link Category} {@link List}.
     * 
     * @param event
     */
    @ConsumeEvent(EventType.Constants.PURGE_AND_PROCESS_CATEGORIES_ADDRESS)
    void consumePurgeAndProcessCategoriesEvent(BackendEvent event) {
        processCategoryList(event.getCategoryList(), true);
    }

    /**
     * Consumes a {@link BackendEvent} to trigger processing of the {@link Category}
     * {@link List}.
     * 
     * @param event
     */
    @ConsumeEvent(EventType.Constants.PROCESS_CATEGORIES_ADDRESS)
    void consumeProcessCategoriesEvent(BackendEvent event) {
        processCategoryList(event.getCategoryList(), false);
    }

    /**
     * Uses the existing persisted {@link Category}s to determine if a {@link Category}
     * needs to be created, deleted, or have its count adjusted.
     * 
     * @param categoryList
     * @param purgeFirst
     */
    void processCategoryList(List<Category> categoryList, boolean purgeFirst) {

        if(purgeFirst) {
            LOGGER.debug("purging categories before processing.");
            repository.deleteAll();
        }

        List<Category> persisted = repository.listAll();

        // create/increment list
        Optional<List<Category>> optional = inListAButNotListB(categoryList, persisted);
        if(optional.isPresent()) {

            LOGGER.debug("processing create/increment category list {}", optional.get());
            optional.get().stream()
                .forEach(category -> {
                    createOrIncrementCategory(category);
                });

        }

        // delete/decrement list
        optional = inListAButNotListB(persisted, categoryList);
        if(optional.isPresent()) {

            LOGGER.debug("processing create/increment category list {}", optional.get());
            optional.get().stream()
                .forEach(category -> {
                    deleteOrDecrementCategory(category);
                });

        }

    }

    /**
     * Creates the {@link Category} if it does not exist.  Otherwise, increments the count by 1.
     * @param category
     */
    void createOrIncrementCategory(Category category) {

        // get if exists
        Optional<Category> optional = get(category.getName(), false);
        if(optional.isPresent()) {

            // increment and update
            Category c = optional.get();
            c.setCount(c.getCount() + 1);
            repository.update(c);
            LOGGER.debug("incremented category {}", c);

        } else {

            // create
            category.setCount(1);
            repository.persist(category);
            LOGGER.debug("created category {}", category);

        }

    }

    /**
     * Deletes the {@link Category} if the count becomes 0.  Otherwise decrements the count by 1.
     *   
     * @param category
     */
    void deleteOrDecrementCategory(Category category) {

        // get existing category
        Optional<Category> optional = get(category.getName(), false);

        if(optional.isPresent()) {

            Category persisted = optional.get();
            Integer adjustedCount = (null == persisted.getCount()) ? 0 : persisted.getCount() - 1;

            if(adjustedCount > 0) {

                // update count
                persisted.setCount(adjustedCount);
                repository.update(persisted);
                LOGGER.debug("decremented category {}", persisted);

            } else {

                // remove category
                persisted.delete();
                LOGGER.debug("deleted category {}", category);

            }

        }

    }

    /**
     * Helper method that eturns an {@link Optional} containing a {@link List} of 
     * {@link Category} in {@link List} A but not in {@link List} B.
     * 
     * @param a
     * @param b
     * @return
     */
    private Optional<List<Category>> inListAButNotListB(List<Category> a, List<Category> b) {

        // return if list a not instantiated
        if (a == null) {
            return Optional.empty();
        }

        List<Category> results =

                a.stream().filter(category -> {

                    // keep if list b is not instantiated
                    if (null == b) {
                        return true;
                    }

                    // validate category is not in list b
                    Optional<Category> optional = b.stream()
                            .filter(uCategory -> category.getName().equalsIgnoreCase(uCategory.getName())).findFirst();

                    return optional.isEmpty();

                }).collect(Collectors.toList());

        return Optional.of(results);

    }

}
