package com.rlnkoo.userservice.api.auth;

import com.rlnkoo.userservice.domain.service.ActivationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/test")
@RequiredArgsConstructor
@Profile({"dev", "test", "local"})
public class TestAuthController {

    private final ActivationService activationService;

    @PostMapping("/activate")
    public void activate(@RequestBody TestActivateRequest request) {
        activationService.activateForTests(request.email());
    }

    public record TestActivateRequest(String email) {}
}