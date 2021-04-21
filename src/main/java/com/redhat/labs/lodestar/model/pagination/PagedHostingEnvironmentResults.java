package com.redhat.labs.lodestar.model.pagination;

import java.util.List;

import com.redhat.labs.lodestar.model.HostingEnvironment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PagedHostingEnvironmentResults extends PagedResults {
   
    private List<HostingEnvironment> results;

}