package com.rlnkoo.listingservice.api.admin;

import com.rlnkoo.listingservice.api.admin.dto.PublishedListingsPageResponse;
import com.rlnkoo.listingservice.domain.service.AdminListingQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminListingController {

    private final AdminListingQueryService adminListingQueryService;

    @GetMapping("/listings/published")
    @PreAuthorize("hasRole('ADMIN')")
    public PublishedListingsPageResponse getPublishedListingsForReindex(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "100") int size
    ) {
        log.info("Admin published listings request received page=[{}] size=[{}]", page, size);
        return adminListingQueryService.getPublishedListingsForReindex(page, size);
    }
}