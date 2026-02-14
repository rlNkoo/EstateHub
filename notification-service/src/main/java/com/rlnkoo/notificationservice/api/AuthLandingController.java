package com.rlnkoo.notificationservice.api;

import com.rlnkoo.notificationservice.api.dto.ConfirmRegistrationRequest;
import com.rlnkoo.notificationservice.api.dto.PasswordResetConfirmRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Controller
@RequiredArgsConstructor
public class AuthLandingController {

    private final RestClient gatewayRestClient;

    @GetMapping("/activate")
    public String activate(@RequestParam @NotBlank String token, Model model) {
        try {
            gatewayRestClient.post()
                    .uri("/auth/confirm-registration")
                    .body(new ConfirmRegistrationRequest(token))
                    .retrieve()
                    .toBodilessEntity();

            model.addAttribute("title", "Account activated");
            model.addAttribute("message", "Your account has been activated successfully. You can now log in.");
            return "result";

        } catch (RestClientResponseException ex) {
            model.addAttribute("title", "Activation failed");
            model.addAttribute("message", ex.getStatusCode().value() + " " + ex.getResponseBodyAsString());
            return "result";
        } catch (Exception ex) {
            model.addAttribute("title", "Activation failed");
            model.addAttribute("message", ex.getMessage());
            return "result";
        }
    }

    @GetMapping("/reset-password")
    public String resetPasswordForm(@RequestParam @NotBlank String token, Model model) {
        model.addAttribute("token", token);
        return "reset-password-form";
    }

    @PostMapping("/reset-password/confirm")
    public String resetPasswordConfirm(
            @RequestParam @NotBlank String token,
            @RequestParam @NotBlank String newPassword,
            Model model
    ) {
        try {
            gatewayRestClient.post()
                    .uri("/auth/password-reset/confirm")
                    .body(new PasswordResetConfirmRequest(token, newPassword))
                    .retrieve()
                    .toBodilessEntity();

            model.addAttribute("title", "Password changed");
            model.addAttribute("message", "Your password has been changed successfully.");
            return "result";

        } catch (RestClientResponseException ex) {
            model.addAttribute("title", "Password change failed");
            model.addAttribute("message", ex.getStatusCode().value() + " " + ex.getResponseBodyAsString());
            return "result";
        } catch (Exception ex) {
            model.addAttribute("title", "Password change failed");
            model.addAttribute("message", ex.getMessage());
            return "result";
        }
    }
}