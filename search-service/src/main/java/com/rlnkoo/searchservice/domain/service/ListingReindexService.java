package com.rlnkoo.searchservice.domain.service;

import com.rlnkoo.searchservice.api.admin.dto.ReindexResponse;
import com.rlnkoo.searchservice.domain.exception.ReindexFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
public class ListingReindexService {

    public ReindexResponse reindexAllPublishedListings() {
        log.info("Reindex all published listings requested");

        log.warn("Reindex requested but listing-service reindex source is not implemented yet");

        throw new ReindexFailedException(
                "Reindex is not available yet. Listing-service published listings endpoint is not implemented"
        );
    }
}