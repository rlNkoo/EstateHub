package com.rlnkoo.searchservice.domain.service;

import com.rlnkoo.searchservice.api.search.dto.SearchFacetBucketResponse;
import com.rlnkoo.searchservice.api.search.dto.SearchFacetsResponse;
import com.rlnkoo.searchservice.api.search.dto.SearchListingItemResponse;
import com.rlnkoo.searchservice.api.search.dto.SearchListingsRequest;
import com.rlnkoo.searchservice.api.search.dto.SearchListingsResponse;
import com.rlnkoo.searchservice.config.SearchProperties;
import com.rlnkoo.searchservice.domain.exception.InvalidSearchRequestException;
import com.rlnkoo.searchservice.domain.exception.SearchListingNotFoundException;
import com.rlnkoo.searchservice.domain.model.SearchListingDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
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

        Query searchQuery = buildSearchQuery(request, page, size, sort);

        log.info(
                "Search request q=[{}] city=[{}] country=[{}] propertyType=[{}] page=[{}] size=[{}] sort=[{}]",
                request.q(), request.city(), request.country(), request.propertyType(), page, size, sort
        );

        SearchHits<SearchListingDocument> searchHits =
                elasticsearchOperations.search(searchQuery, SearchListingDocument.class);

        List<SearchListingItemResponse> items = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::mapToItemResponse)
                .toList();

        long totalElements = searchHits.getTotalHits();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);

        SearchFacetsResponse facets = buildFacets(request);

        log.info("Search completed totalElements=[{}] page=[{}] size=[{}]", totalElements, page, size);

        return SearchListingsResponse.builder()
                .items(items)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .page(page)
                .size(size)
                .sort(sort)
                .facets(facets)
                .build();
    }

    public SearchListingItemResponse getById(UUID listingId) {
        SearchListingDocument document = elasticsearchOperations.get(
                listingId.toString(),
                SearchListingDocument.class
        );

        if (document == null) {
            log.warn("Search listing not found listingId=[{}]", listingId);
            throw new SearchListingNotFoundException(listingId);
        }

        if (!STATUS_PUBLISHED.equalsIgnoreCase(document.getStatus())) {
            log.warn("Search listing found but not published listingId=[{}] status=[{}]",
                    listingId, document.getStatus());
            throw new SearchListingNotFoundException(listingId);
        }

        return mapToItemResponse(document);
    }

    private Query buildSearchQuery(SearchListingsRequest request, int page, int size, String sort) {
        return NativeQuery.builder()
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
    }

    private SearchFacetsResponse buildFacets(SearchListingsRequest request) {
        Query facetQuery = buildFacetBaseQuery(request);

        SearchHits<SearchListingDocument> hits =
                elasticsearchOperations.search(facetQuery, SearchListingDocument.class);

        List<SearchListingDocument> documents = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        List<SearchFacetBucketResponse> cities = documents.stream()
                .map(SearchListingDocument::getCity)
                .filter(this::hasText)
                .collect(java.util.stream.Collectors.groupingBy(city -> city, java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .sorted(
                        Comparator.<java.util.Map.Entry<String, Long>>comparingLong(java.util.Map.Entry::getValue)
                                .reversed()
                                .thenComparing(java.util.Map.Entry::getKey)
                )
                .map(entry -> SearchFacetBucketResponse.builder()
                        .value(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .toList();

        List<SearchFacetBucketResponse> propertyTypes = documents.stream()
                .map(SearchListingDocument::getPropertyType)
                .filter(this::hasText)
                .collect(java.util.stream.Collectors.groupingBy(type -> type, java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .sorted(
                        Comparator.<java.util.Map.Entry<String, Long>>comparingLong(java.util.Map.Entry::getValue)
                                .reversed()
                                .thenComparing(java.util.Map.Entry::getKey)
                )
                .map(entry -> SearchFacetBucketResponse.builder()
                        .value(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .toList();

        List<SearchFacetBucketResponse> rooms = documents.stream()
                .map(SearchListingDocument::getRooms)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.groupingBy(String::valueOf, java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .sorted(
                        Comparator.<java.util.Map.Entry<String, Long>, Integer>comparing(
                                entry -> Integer.parseInt(entry.getKey())
                        )
                )
                .map(entry -> SearchFacetBucketResponse.builder()
                        .value(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .toList();

        return SearchFacetsResponse.builder()
                .cities(cities)
                .propertyTypes(propertyTypes)
                .rooms(rooms)
                .build();
    }

    private Query buildFacetBaseQuery(SearchListingsRequest request) {
        return NativeQuery.builder()
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
                .withPageable(PageRequest.of(0, 1000))
                .build();
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
        List<String> errors = new ArrayList<>();

        if (request.priceFrom() != null && request.priceTo() != null
                && request.priceFrom().compareTo(request.priceTo()) > 0) {
            errors.add("priceFrom must be less than or equal to priceTo");
        }

        if (request.areaFrom() != null && request.areaTo() != null
                && request.areaFrom().compareTo(request.areaTo()) > 0) {
            errors.add("areaFrom must be less than or equal to areaTo");
        }

        if (request.rooms() != null && request.rooms() < 0) {
            errors.add("rooms must be greater than or equal to 0");
        }

        if (request.floor() != null && request.floor() < 0) {
            errors.add("floor must be greater than or equal to 0");
        }

        if (!errors.isEmpty()) {
            throw new InvalidSearchRequestException(String.join("; ", errors));
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}