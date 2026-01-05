package com.rlnkoo.userservice.api.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class ChangeRolesRequest {

    @NotEmpty
    private Set<String> roles;
}