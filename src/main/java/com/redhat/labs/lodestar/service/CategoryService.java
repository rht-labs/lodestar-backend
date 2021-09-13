package com.redhat.labs.lodestar.service;

import com.redhat.labs.lodestar.rest.client.CategoryApiClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Set;

@ApplicationScoped
public class CategoryService {

    @Inject
    @RestClient
    CategoryApiClient categoryApiClient;

    public Set<String> getCategorySuggestions(String partial) {
        return categoryApiClient.getCategorySuggestions(partial);
    }
}
