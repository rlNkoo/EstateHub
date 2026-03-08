package com.rlnkoo.searchservice.domain.service;

import com.rlnkoo.searchservice.api.search.dto.SearchListingItemResponse;
import com.rlnkoo.searchservice.api.search.dto.SearchListingsRequest;
import com.rlnkoo.searchservice.api.search.dto.SearchListingsResponse;
import com.rlnkoo.searchservice.config.SearchProperties;
import com.rlnkoo.searchservice.domain.exception.InvalidSearchRequestException;
import com.rlnkoo.searchservice.domain.model.SearchListingDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListingSearchService {

    private static final String STATUS_PUBLISHED = "PUBLISHED";

    private final ElasticsearchOperations elasticsearchOperations;
    private final SearchProperties searchProperties;

    public SearchListingsResponse search(SearchListingsRequest request) {
        int page = normalizePage(request.page());
        int size = normalizeSize(request.size());
        String sort = normalizeSort(request.sort());

        validateRanges(request);

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> {
                    b.must(m -> m.term(t -> t.field("status").value(STATUS_PUBLISHED)));

                    if (hasText(request.q())) {
                        b.must(m -> m.bool(qb -> qb
                                .should(s -> s.match(mt -> mt.field("title").query(request.q())))
                                .should(s -> s.match(mt -> mt.field("description").query(request.q())))
                                .minimumShouldMatch("1")
                        ));
                    }

                    if (hasText(request.city())) {
                        b.filter(f -> f.term(t -> t.field("city").value(request.city())));
                    }

                    if (hasText(request.country())) {
                        b.filter(f -> f.term(t -> t.field("country").value(request.country())));
                    }

                    if (hasText(request.propertyType())) {
                        b.filter(f -> f.term(t -> t.field("propertyType").value(request.propertyType())));
                    }

                    if (request.rooms() != null) {
                        b.filter(f -> f.term(t -> t.field("rooms").value(request.rooms())));
                    }

                    if (request.floor() != null) {
                        b.filter(f -> f.term(t -> t.field("floor").value(request.floor())));
                    }

                    if (request.priceFrom() != null || request.priceTo() != null) {
                        b.filter(f -> f.range(r -> {
                            r.number(n -> {
                                n.field("priceAmount");
                                if (request.priceFrom() != null) {
                                    n.gte(request.priceFrom().doubleValue());
                                }
                                if (request.priceTo() != null) {
                                    n.lte(request.priceTo().doubleValue());
                                }
                                return n;
                            });
                            return r;
                        }));
                    }

                    if (request.areaFrom() != null || request.areaTo() != null) {
                        b.filter(f -> f.range(r -> {
                            r.number(n -> {
                                n.field("area");
                                if (request.areaFrom() != null) {
                                    n.gte(request.areaFrom().doubleValue());
                                }
                                if (request.areaTo() != null) {
                                    n.lte(request.areaTo().doubleValue());
                                }
                                return n;
                            });
                            return r;
                        }));
                    }

                    return b;
                }))
                .withPageable(PageRequest.of(page, size))
                .withSort(resolveSort(sort))
                .build();

        log.info(
                "Search request q=[{}] city=[{}] country=[{}] propertyType=[{}] page=[{}] size=[{}] sort=[{}]",
                request.q(), request.city(), request.country(), request.propertyType(), page, size, sort
        );

        SearchHits<SearchListingDocument> searchHits =
                elasticsearchOperations.search(query, SearchListingDocument.class);

        List<SearchListingItemResponse> items = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::mapToItemResponse)
                .toList();

        long totalElements = searchHits.getTotalHits();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);

        log.info("Search completed totalElements=[{}] page=[{}] size=[{}]", totalElements, page, size);

        return SearchListingsResponse.builder()
                .items(items)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .page(page)
                .size(size)
                .sort(sort)
                .build();
    }

    public SearchListingItemResponse getByIdOrNull(UUID listingId) {
        SearchListingDocument document = elasticsearchOperations.get(
                listingId.toString(),
                SearchListingDocument.class
        );

        if (document == null) {
            log.debug("Search listing not found listingId=[{}]", listingId);
            return null;
        }

        if (!STATUS_PUBLISHED.equalsIgnoreCase(document.getStatus())) {
            log.debug("Search listing found but not published listingId=[{}] status=[{}]",
                    listingId, document.getStatus());
            return null;
        }

        return mapToItemResponse(document);
    }

    private SearchListingItemResponse mapToItemResponse(SearchListingDocument document) {
        return SearchListingItemResponse.builder()
                .id(document.getId())
                .title(document.getTitle())
                .description(document.getDescription())
                .priceAmount(document.getPriceAmount())
                .currencyCode(document.getCurrencyCode())
                .country(document.getCountry())
                .city(document.getCity())
                .street(document.getStreet())
                .postalCode(document.getPostalCode())
                .area(document.getArea())
                .rooms(document.getRooms())
                .floor(document.getFloor())
                .propertyType(document.getPropertyType())
                .photoIds(document.getPhotoIds())
                .publishedAt(document.getPublishedAt())
                .build();
    }

    private int normalizePage(Integer page) {
        if (page == null) {
            return 0;
        }
        if (page < 0) {
            throw new InvalidSearchRequestException("page must be greater than or equal to 0");
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return searchProperties.getDefaultPageSize();
        }
        if (size < 1) {
            throw new InvalidSearchRequestException("size must be greater than 0");
        }
        if (size > searchProperties.getMaxPageSize()) {
            throw new InvalidSearchRequestException(
                    "size must be less than or equal to " + searchProperties.getMaxPageSize()
            );
        }
        return size;
    }

    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "publishedAt,desc";
        }

        return switch (sort) {
            case "publishedAt,desc",
                 "priceAmount,asc",
                 "priceAmount,desc",
                 "area,asc",
                 "area,desc" -> sort;
            default -> throw new InvalidSearchRequestException("Unsupported sort: " + sort);
        };
    }

    private Sort resolveSort(String sort) {
        return switch (sort) {
            case "priceAmount,asc" -> Sort.by(Sort.Order.asc("priceAmount"));
            case "priceAmount,desc" -> Sort.by(Sort.Order.desc("priceAmount"));
            case "area,asc" -> Sort.by(Sort.Order.asc("area"));
            case "area,desc" -> Sort.by(Sort.Order.desc("area"));
            default -> Sort.by(Sort.Order.desc("publishedAt"));
        };
    }

    private void validateRanges(SearchListingsRequest request) {
        if (request.priceFrom() != null && request.priceTo() != null
                && request.priceFrom().compareTo(request.priceTo()) > 0) {
            throw new InvalidSearchRequestException("priceFrom must be less than or equal to priceTo");
        }

        if (request.areaFrom() != null && request.areaTo() != null
                && request.areaFrom().compareTo(request.areaTo()) > 0) {
            throw new InvalidSearchRequestException("areaFrom must be less than or equal to areaTo");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}