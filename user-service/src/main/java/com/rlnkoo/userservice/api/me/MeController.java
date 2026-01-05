package com.rlnkoo.userservice.api.me;

import com.rlnkoo.userservice.api.me.dto.MeResponse;
import com.rlnkoo.userservice.domain.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/me")
public class MeController {

    private final UserProfileService userProfileService;

    @GetMapping
    public MeResponse me() {
        return userProfileService.getMe();
    }
}