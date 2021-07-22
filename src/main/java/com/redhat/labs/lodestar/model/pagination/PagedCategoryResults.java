package com.redhat.labs.lodestar.model.pagination;

import com.redhat.labs.lodestar.model.Category;

import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
public class PagedCategoryResults extends PagedResults<Category> {

}
