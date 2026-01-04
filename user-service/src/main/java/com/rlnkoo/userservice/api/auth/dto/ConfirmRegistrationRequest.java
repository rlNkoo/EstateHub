package com.rlnkoo.userservice.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmRegistrationRequest {

    @NotBlank
    private String token;
}