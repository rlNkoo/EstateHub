package com.rlnkoo.searchservice.domain.service;

import com.rlnkoo.searchservice.api.admin.dto.ReindexResponse;
import com.rlnkoo.searchservice.config.ReindexProperties;
import com.rlnkoo.searchservice.domain.exception.ReindexFailedException;
import com.rlnkoo.searchservice.domain.model.SearchListingDocument;
import com.rlnkoo.searchservice.integration.listing.ListingServiceClient;
import com.rlnkoo.searchservice.integration.listing.dto.PublishedListingForReindexResponse;
import com.rlnkoo.searchservice.integration.listing.dto.PublishedListingsPageResponse;
import com.rlnkoo.searchservice.persistence.repository.SearchListingRepository;
import com.rlnkoo.searchservice.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListingReindexService {

    private final ListingServiceClient listingServiceClient;
    private final SearchListingRepository searchListingRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ReindexProperties reindexProperties;

    public ReindexResponse reindexAllPublishedListings() {
        String bearerToken = currentUserProvider.requireCurrentJwt().getTokenValue();
        int pageSize = reindexProperties.getPageSize();

        log.info("Reindex all published listings started pageSize=[{}]", pageSize);

        int page = 0;
        int processedPages = 0;
        int fetchedCount = 0;
        int indexedCount = 0;
        int failedCount = 0;

        try {
            while (true) {
                PublishedListingsPageResponse response =
                        listingServiceClient.getPublishedListingsForReindex(page, pageSize, bearerToken);

                List<PublishedListingForReindexResponse> items =
                        response.items() == null ? List.of() : response.items();

                if (items.isEmpty()) {
                    log.info("No more published listings to reindex page=[{}]", page);
                    break;
                }

                fetchedCount += items.size();

                List<SearchListingDocument> documents = items.stream()
                        .map(this::mapToDocument)
                        .toList();

                searchListingRepository.saveAll(documents);
                indexedCount += documents.size();
                processedPages++;

                log.info("Reindex page processed page=[{}] items=[{}] indexedCount=[{}]",
                        page, items.size(), indexedCount);

                if (page >= response.totalPages() - 1) {
                    break;
                }

                page++;
            }

            log.info("Reindex completed fetchedCount=[{}] indexedCount=[{}] failedCount=[{}] processedPages=[{}]",
                    fetchedCount, indexedCount, failedCount, processedPages);

            return ReindexResponse.builder()
                    .completed(true)
                    .message("Reindex completed successfully")
                    .fetchedCount(fetchedCount)
                    .indexedCount(indexedCount)
                    .failedCount(failedCount)
                    .processedPages(processedPages)
                    .timestamp(Instant.now())
                    .build();

        } catch (Exception ex) {
            log.error("Reindex failed after processedPages=[{}] fetchedCount=[{}] indexedCount=[{}]",
                    processedPages, fetchedCount, indexedCount, ex);

            throw new ReindexFailedException("Failed to reindex published listings", ex);
        }
    }

    private SearchListingDocument mapToDocument(PublishedListingForReindexResponse item) {
        return SearchListingDocument.builder()
                .id(item.id())
                .ownerId(item.ownerId())
                .status(item.status())
                .version(item.version())
                .publishedAt(item.publishedAt())
                .title(item.title())
                .description(item.description())
                .priceAmount(item.priceAmount())
                .currencyCode(item.currencyCode())
                .country(item.address() != null ? item.address().country() : null)
                .city(item.address() != null ? item.address().city() : null)
                .street(item.address() != null ? item.address().street() : null)
                .postalCode(item.address() != null ? item.address().postalCode() : null)
                .area(item.area())
                .rooms(item.rooms())
                .floor(item.floor())
                .propertyType(item.propertyType())
                .photoIds(item.photoIds() == null ? List.of() : List.copyOf(item.photoIds()))
                .indexedAt(Instant.now())
                .build();
    }
}