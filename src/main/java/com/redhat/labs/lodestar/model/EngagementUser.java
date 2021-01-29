package com.redhat.labs.lodestar.model;

import javax.json.bind.annotation.JsonbProperty;
import javax.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class EngagementUser {

    @NotBlank
    @JsonbProperty("first_name")
    private String firstName;
    @NotBlank
    @JsonbProperty("last_name")
    private String lastName;
    @NotBlank
    private String email;
    @NotBlank
    private String role;
    private String uuid;
    private boolean reset;

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EngagementUser other = (EngagementUser) obj;
        if ((email == null && other.email != null) || (email != null && other.email == null)) {
            return false;
        }
        return email.equals(other.email);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((email == null) ? 0 : email.hashCode());
        return result;
    }

}
