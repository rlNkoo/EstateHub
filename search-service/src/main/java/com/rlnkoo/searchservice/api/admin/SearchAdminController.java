package com.rlnkoo.searchservice.api.admin;

import com.rlnkoo.searchservice.api.admin.dto.IndexInfoResponse;
import com.rlnkoo.searchservice.api.admin.dto.ReindexResponse;
import com.rlnkoo.searchservice.domain.service.IndexDiagnosticsService;
import com.rlnkoo.searchservice.domain.service.ListingReindexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/search/admin")
@RequiredArgsConstructor
public class SearchAdminController {

    private final ListingReindexService listingReindexService;
    private final IndexDiagnosticsService indexDiagnosticsService;

    @GetMapping("/index-info")
    @PreAuthorize("hasRole('ADMIN')")
    public IndexInfoResponse getIndexInfo() {
        log.info("Admin index-info request received");
        return indexDiagnosticsService.getIndexInfo();
    }

    @PostMapping("/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public ReindexResponse reindexAllPublishedListings() {
        log.info("Admin reindex request received");
        return listingReindexService.reindexAllPublishedListings();
    }
}