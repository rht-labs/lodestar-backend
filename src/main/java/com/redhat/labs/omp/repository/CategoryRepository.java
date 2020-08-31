package com.redhat.labs.omp.repository;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.labs.omp.model.Category;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import io.quarkus.panache.common.Sort;

@ApplicationScoped
public class CategoryRepository implements PanacheMongoRepository<Category> {

    /**
     * A case insensitive string to match against category names.
     * 
     * @param input
     * @return
     */
    public List<Category> searchCategories(String input) {

        String queryInput = String.format("(?i)%s", input);
        return find("name like ?1", Sort.ascending("name"), queryInput).list();

    }

    /**
     * A case insensitive string to match against exact category name.
     * 
     * @param name
     * @param matchCase
     * @return
     */
    public Optional<Category> findCategory(String name, boolean matchCase) {

        String queryInput = matchCase ? String.format("(?i)%s", name) : name;
        return find("name", queryInput).singleResultOptional();

    }

}
