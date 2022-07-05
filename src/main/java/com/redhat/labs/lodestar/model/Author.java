package com.redhat.labs.lodestar.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Author {

    private String name;
    private String email;
}
