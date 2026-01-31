package com.rlnkoo.mediaservice.api.internal;

import com.rlnkoo.mediaservice.api.internal.dto.ValidatePhotoOwnershipRequest;
import com.rlnkoo.mediaservice.api.internal.dto.ValidatePhotoOwnershipResponse;
import com.rlnkoo.mediaservice.domain.service.MediaValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/media")
public class MediaInternalController {

    private final MediaValidationService mediaValidationService;

    @PostMapping("/validate-ownership")
    public ResponseEntity<ValidatePhotoOwnershipResponse> validateOwnership(
            @RequestBody ValidatePhotoOwnershipRequest request
    ) {
        log.info("Internal validate ownership request requesterId=[{}] photoIdsCount=[{}]",
                request.requesterId(),
                request.photoIds() == null ? 0 : request.photoIds().size()
        );

        ValidatePhotoOwnershipResponse response = mediaValidationService.validate(request);
        return ResponseEntity.ok(response);
    }
}