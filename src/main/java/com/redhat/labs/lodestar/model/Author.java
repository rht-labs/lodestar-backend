package com.redhat.labs.lodestar.model;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Author {

    private String name;
    private String email;
}
