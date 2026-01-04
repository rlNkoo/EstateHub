package com.rlnkoo.userservice.api.auth;

import com.rlnkoo.userservice.api.auth.dto.ConfirmRegistrationRequest;
import com.rlnkoo.userservice.api.auth.dto.ConfirmRegistrationResponse;
import com.rlnkoo.userservice.api.auth.dto.RegisterRequest;
import com.rlnkoo.userservice.api.auth.dto.RegisterResponse;
import com.rlnkoo.userservice.domain.service.ActivationService;
import com.rlnkoo.userservice.domain.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegistrationService registrationService;
    private final ActivationService activationService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        registrationService.register(
                request.getEmail(),
                request.getPassword()
        );

        return RegisterResponse.builder()
                .message("Registration successful. Please confirm your email address.")
                .activationRequired(true)
                .build();
    }

    @PostMapping("/confirm-registration")
    @ResponseStatus(HttpStatus.OK)
    public ConfirmRegistrationResponse confirmRegistration(@Valid @RequestBody ConfirmRegistrationRequest request) {
        activationService.confirmRegistration(request.getToken());

        return ConfirmRegistrationResponse.builder()
                .message("Account activated successfully. You can now log in.")
                .build();
    }
}