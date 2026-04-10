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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingSearchServiceTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private SearchProperties searchProperties;

    @InjectMocks
    private ListingSearchService listingSearchService;

    @Test
    void shouldUseDefaultPageAndSizeAndSortWhenNotProvided() {
        // given
        when(searchProperties.getDefaultPageSize()).thenReturn(20);

        SearchListingDocument document = SearchListingDocument.builder()
                .id(UUID.randomUUID())
                .status("PUBLISHED")
                .title("Modern apartment")
                .description("Nice apartment in city center")
                .priceAmount(new BigDecimal("500000.00"))
                .currencyCode("PLN")
                .country("Poland")
                .city("Warsaw")
                .street("Main Street")
                .postalCode("00-001")
                .area(new BigDecimal("55.50"))
                .rooms(3)
                .floor(2)
                .propertyType("APARTMENT")
                .photoIds(List.of())
                .publishedAt(Instant.parse("2025-01-10T10:15:30Z"))
                .build();

        SearchHits<SearchListingDocument> searchHits = searchHits(List.of(document), 1);
        SearchHits<SearchListingDocument> facetHits = facetSearchHits(List.of(document));

        when(elasticsearchOperations.search(any(Query.class), eq(SearchListingDocument.class)))
                .thenReturn(searchHits, facetHits);

        SearchListingsRequest request = SearchListingsRequest.builder().build();

        // when
        SearchListingsResponse response = listingSearchService.search(request);

        // then
        assertNotNull(response);
        assertEquals(1, response.items().size());
        assertEquals(1L, response.totalElements());
        assertEquals(1, response.totalPages());
        assertEquals(0, response.page());
        assertEquals(20, response.size());
        assertEquals("publishedAt,desc", response.sort());

        SearchListingItemResponse item = response.items().getFirst();
        assertEquals(document.getId(), item.id());
        assertEquals("Modern apartment", item.title());
        assertEquals("500000", item.priceAmount());
        assertEquals("PLN", item.currencyCode());
        assertEquals("Warsaw", item.city());

        verify(searchProperties).getDefaultPageSize();
        verify(searchProperties, never()).getMaxPageSize();
        verify(elasticsearchOperations, times(2)).search(any(Query.class), eq(SearchListingDocument.class));
    }

    @Test
    void shouldReturnSearchResponseWithMappedItemsAndFacets() {
        // given
        when(searchProperties.getMaxPageSize()).thenReturn(100);

        SearchListingDocument first = SearchListingDocument.builder()
                .id(UUID.randomUUID())
                .status("PUBLISHED")
                .title("Apartment A")
                .description("Description A")
                .priceAmount(new BigDecimal("450000.00"))
                .currencyCode("PLN")
                .country("Poland")
                .city("Warsaw")
                .street("Street A")
                .postalCode("00-001")
                .area(new BigDecimal("48.50"))
                .rooms(2)
                .floor(1)
                .propertyType("APARTMENT")
                .photoIds(List.of(UUID.randomUUID()))
                .publishedAt(Instant.parse("2025-02-01T08:00:00Z"))
                .build();

        SearchListingDocument second = SearchListingDocument.builder()
                .id(UUID.randomUUID())
                .status("PUBLISHED")
                .title("House B")
                .description("Description B")
                .priceAmount(new BigDecimal("950000.50"))
                .currencyCode("PLN")
                .country("Poland")
                .city("London")
                .street("Street B")
                .postalCode("30-001")
                .area(new BigDecimal("120.00"))
                .rooms(4)
                .floor(0)
                .propertyType("HOUSE")
                .photoIds(List.of(UUID.randomUUID(), UUID.randomUUID()))
                .publishedAt(Instant.parse("2025-02-02T08:00:00Z"))
                .build();

        SearchHits<SearchListingDocument> searchHits = searchHits(List.of(first, second), 2);
        SearchHits<SearchListingDocument> facetHits = facetSearchHits(List.of(first, second));

        when(elasticsearchOperations.search(any(Query.class), eq(SearchListingDocument.class)))
                .thenReturn(searchHits, facetHits);

        SearchListingsRequest request = SearchListingsRequest.builder()
                .q("property")
                .city("Warsaw")
                .country("Poland")
                .propertyType("APARTMENT")
                .priceFrom(new BigDecimal("100000"))
                .priceTo(new BigDecimal("1000000"))
                .areaFrom(new BigDecimal("20"))
                .areaTo(new BigDecimal("150"))
                .rooms(2)
                .floor(1)
                .page(1)
                .size(10)
                .sort("priceAmount,asc")
                .build();

        // when
        SearchListingsResponse response = listingSearchService.search(request);

        // then
        assertNotNull(response);
        assertEquals(2, response.items().size());
        assertEquals(2L, response.totalElements());
        assertEquals(1, response.totalPages());
        assertEquals(1, response.page());
        assertEquals(10, response.size());
        assertEquals("priceAmount,asc", response.sort());

        SearchListingItemResponse firstItem = response.items().get(0);
        assertEquals(first.getId(), firstItem.id());
        assertEquals("450000", firstItem.priceAmount());

        SearchListingItemResponse secondItem = response.items().get(1);
        assertEquals(second.getId(), secondItem.id());
        assertEquals("950000.5", secondItem.priceAmount());

        SearchFacetsResponse facets = response.facets();
        assertNotNull(facets);

        assertEquals(
                List.of(
                        SearchFacetBucketResponse.builder().value("London").count(1).build(),
                        SearchFacetBucketResponse.builder().value("Warsaw").count(1).build()
                ),
                facets.cities()
        );

        assertEquals(
                List.of(
                        SearchFacetBucketResponse.builder().value("APARTMENT").count(1).build(),
                        SearchFacetBucketResponse.builder().value("HOUSE").count(1).build()
                ),
                facets.propertyTypes()
        );

        assertEquals(
                List.of(
                        SearchFacetBucketResponse.builder().value("2").count(1).build(),
                        SearchFacetBucketResponse.builder().value("4").count(1).build()
                ),
                facets.rooms()
        );

        verify(elasticsearchOperations, times(2)).search(any(Query.class), eq(SearchListingDocument.class));
    }

    @Test
    void shouldReturnZeroTotalPagesWhenNoResults() {
        // given
        when(searchProperties.getMaxPageSize()).thenReturn(100);

        SearchHits<SearchListingDocument> searchHits = searchHits(List.of(), 0);
        SearchHits<SearchListingDocument> facetHits = facetSearchHits(List.of());

        when(elasticsearchOperations.search(any(Query.class), eq(SearchListingDocument.class)))
                .thenReturn(searchHits, facetHits);

        SearchListingsRequest request = SearchListingsRequest.builder()
                .page(0)
                .size(20)
                .build();

        // when
        SearchListingsResponse response = listingSearchService.search(request);

        // then
        assertNotNull(response);
        assertTrue(response.items().isEmpty());
        assertEquals(0L, response.totalElements());
        assertEquals(0, response.totalPages());
        assertEquals(0, response.page());
        assertEquals(20, response.size());
        assertNotNull(response.facets());
        assertTrue(response.facets().cities().isEmpty());
        assertTrue(response.facets().propertyTypes().isEmpty());
        assertTrue(response.facets().rooms().isEmpty());
    }

    @Test
    void shouldCalculateTotalPagesCorrectly() {
        // given
        when(searchProperties.getMaxPageSize()).thenReturn(100);

        SearchListingDocument document = SearchListingDocument.builder()
                .id(UUID.randomUUID())
                .status("PUBLISHED")
                .title("Listing")
                .build();

        SearchHits<SearchListingDocument> searchHits = searchHits(List.of(document), 21);
        SearchHits<SearchListingDocument> facetHits = facetSearchHits(List.of(document));

        when(elasticsearchOperations.search(any(Query.class), eq(SearchListingDocument.class)))
                .thenReturn(searchHits, facetHits);

        SearchListingsRequest request = SearchListingsRequest.builder()
                .page(0)
                .size(10)
                .build();

        // when
        SearchListingsResponse response = listingSearchService.search(request);

        // then
        assertEquals(21L, response.totalElements());
        assertEquals(3, response.totalPages());
    }

    @Test
    void shouldBuildFacetsSortedByCountAndValueAndIgnoreBlankValues() {
        // given
        when(searchProperties.getDefaultPageSize()).thenReturn(20);

        SearchListingDocument searchDocument = SearchListingDocument.builder()
                .id(UUID.randomUUID())
                .status("PUBLISHED")
                .title("Listing")
                .build();

        SearchListingDocument first = SearchListingDocument.builder()
                .id(UUID.randomUUID())
                .city("Warsaw")
                .propertyType("HOUSE")
                .rooms(3)
                .build();

        SearchListingDocument second = SearchListingDocument.builder()
                .id(UUID.randomUUID())
                .city("Warsaw")
                .propertyType("APARTMENT")
                .rooms(2)
                .build();

        SearchListingDocument third = SearchListingDocument.builder()
                .id(UUID.randomUUID())
                .city("London")
                .propertyType("APARTMENT")
                .rooms(10)
                .build();

        SearchListingDocument fourth = SearchListingDocument.builder()
                .id(UUID.randomUUID())
                .city(" ")
                .propertyType("")
                .rooms(null)
                .build();

        SearchHits<SearchListingDocument> searchHits = searchHits(List.of(searchDocument), 1);
        SearchHits<SearchListingDocument> facetHits = facetSearchHits(List.of(first, second, third, fourth));

        when(elasticsearchOperations.search(any(Query.class), eq(SearchListingDocument.class)))
                .thenReturn(searchHits, facetHits);

        SearchListingsRequest request = SearchListingsRequest.builder().build();

        // when
        SearchListingsResponse response = listingSearchService.search(request);

        // then
        assertNotNull(response.facets());

        assertEquals(
                List.of(
                        SearchFacetBucketResponse.builder().value("Warsaw").count(2).build(),
                        SearchFacetBucketResponse.builder().value("London").count(1).build()
                ),
                response.facets().cities()
        );

        assertEquals(
                List.of(
                        SearchFacetBucketResponse.builder().value("APARTMENT").count(2).build(),
                        SearchFacetBucketResponse.builder().value("HOUSE").count(1).build()
                ),
                response.facets().propertyTypes()
        );

        assertEquals(
                List.of(
                        SearchFacetBucketResponse.builder().value("2").count(1).build(),
                        SearchFacetBucketResponse.builder().value("3").count(1).build(),
                        SearchFacetBucketResponse.builder().value("10").count(1).build()
                ),
                response.facets().rooms()
        );
    }

    @Test
    void shouldThrowInvalidSearchRequestExceptionWhenPageIsNegative() {
        // given
        SearchListingsRequest request = SearchListingsRequest.builder()
                .page(-1)
                .build();

        // when + then
        InvalidSearchRequestException exception = assertThrows(
                InvalidSearchRequestException.class,
                () -> listingSearchService.search(request)
        );

        assertEquals("page must be greater than or equal to 0", exception.getMessage());
        verifyNoInteractions(elasticsearchOperations);
    }

    @Test
    void shouldThrowInvalidSearchRequestExceptionWhenSizeIsLessThanOne() {
        // given
        SearchListingsRequest request = SearchListingsRequest.builder()
                .size(0)
                .build();

        // when + then
        InvalidSearchRequestException exception = assertThrows(
                InvalidSearchRequestException.class,
                () -> listingSearchService.search(request)
        );

        assertEquals("size must be greater than 0", exception.getMessage());
        verifyNoInteractions(elasticsearchOperations);
    }

    @Test
    void shouldThrowInvalidSearchRequestExceptionWhenSizeExceedsMaxPageSize() {
        // given
        when(searchProperties.getMaxPageSize()).thenReturn(100);

        SearchListingsRequest request = SearchListingsRequest.builder()
                .size(101)
                .build();

        // when + then
        InvalidSearchRequestException exception = assertThrows(
                InvalidSearchRequestException.class,
                () -> listingSearchService.search(request)
        );

        assertEquals("size must be less than or equal to 100", exception.getMessage());
        verify(searchProperties, times(2)).getMaxPageSize();
        verifyNoInteractions(elasticsearchOperations);
    }

    @Test
    void shouldThrowInvalidSearchRequestExceptionWhenSortIsUnsupported() {
        // given
        SearchListingsRequest request = SearchListingsRequest.builder()
                .sort("title,asc")
                .build();

        // when + then
        InvalidSearchRequestException exception = assertThrows(
                InvalidSearchRequestException.class,
                () -> listingSearchService.search(request)
        );

        assertEquals("Unsupported sort: title,asc", exception.getMessage());
        verifyNoInteractions(elasticsearchOperations);
    }

    @Test
    void shouldThrowInvalidSearchRequestExceptionWhenPriceFromIsGreaterThanPriceTo() {
        // given
        SearchListingsRequest request = SearchListingsRequest.builder()
                .priceFrom(new BigDecimal("1000"))
                .priceTo(new BigDecimal("500"))
                .build();

        // when + then
        InvalidSearchRequestException exception = assertThrows(
                InvalidSearchRequestException.class,
                () -> listingSearchService.search(request)
        );

        assertEquals("priceFrom must be less than or equal to priceTo", exception.getMessage());
        verifyNoInteractions(elasticsearchOperations);
    }

    @Test
    void shouldThrowInvalidSearchRequestExceptionWhenAreaFromIsGreaterThanAreaTo() {
        // given
        SearchListingsRequest request = SearchListingsRequest.builder()
                .areaFrom(new BigDecimal("100"))
                .areaTo(new BigDecimal("50"))
                .build();

        // when + then
        InvalidSearchRequestException exception = assertThrows(
                InvalidSearchRequestException.class,
                () -> listingSearchService.search(request)
        );

        assertEquals("areaFrom must be less than or equal to areaTo", exception.getMessage());
        verifyNoInteractions(elasticsearchOperations);
    }

    @Test
    void shouldThrowInvalidSearchRequestExceptionWhenRoomsIsNegative() {
        // given
        SearchListingsRequest request = SearchListingsRequest.builder()
                .rooms(-1)
                .build();

        // when + then
        InvalidSearchRequestException exception = assertThrows(
                InvalidSearchRequestException.class,
                () -> listingSearchService.search(request)
        );

        assertEquals("rooms must be greater than or equal to 0", exception.getMessage());
        verifyNoInteractions(elasticsearchOperations);
    }

    @Test
    void shouldThrowInvalidSearchRequestExceptionWhenFloorIsNegative() {
        // given
        SearchListingsRequest request = SearchListingsRequest.builder()
                .floor(-1)
                .build();

        // when + then
        InvalidSearchRequestException exception = assertThrows(
                InvalidSearchRequestException.class,
                () -> listingSearchService.search(request)
        );

        assertEquals("floor must be greater than or equal to 0", exception.getMessage());
        verifyNoInteractions(elasticsearchOperations);
    }

    @Test
    void shouldThrowInvalidSearchRequestExceptionWhenMultipleValidationErrorsOccur() {
        // given
        SearchListingsRequest request = SearchListingsRequest.builder()
                .priceFrom(new BigDecimal("1000"))
                .priceTo(new BigDecimal("500"))
                .areaFrom(new BigDecimal("80"))
                .areaTo(new BigDecimal("40"))
                .rooms(-2)
                .floor(-3)
                .build();

        // when + then
        InvalidSearchRequestException exception = assertThrows(
                InvalidSearchRequestException.class,
                () -> listingSearchService.search(request)
        );

        assertEquals(
                "priceFrom must be less than or equal to priceTo; " +
                        "areaFrom must be less than or equal to areaTo; " +
                        "rooms must be greater than or equal to 0; " +
                        "floor must be greater than or equal to 0",
                exception.getMessage()
        );

        verifyNoInteractions(elasticsearchOperations);
    }

    @Test
    void shouldReturnListingWhenDocumentExistsAndIsPublished() {
        // given
        UUID listingId = UUID.randomUUID();

        SearchListingDocument document = SearchListingDocument.builder()
                .id(listingId)
                .status("PUBLISHED")
                .title("Premium apartment")
                .description("Great location")
                .priceAmount(new BigDecimal("700000.00"))
                .currencyCode("PLN")
                .country("Poland")
                .city("Warsaw")
                .street("Market Street")
                .postalCode("00-001")
                .area(new BigDecimal("70.00"))
                .rooms(3)
                .floor(5)
                .propertyType("APARTMENT")
                .photoIds(List.of(UUID.randomUUID()))
                .publishedAt(Instant.parse("2025-01-15T12:00:00Z"))
                .build();

        when(elasticsearchOperations.get(listingId.toString(), SearchListingDocument.class)).thenReturn(document);

        // when
        SearchListingItemResponse response = listingSearchService.getById(listingId);

        // then
        assertNotNull(response);
        assertEquals(listingId, response.id());
        assertEquals("Premium apartment", response.title());
        assertEquals("700000", response.priceAmount());
        assertEquals("Warsaw", response.city());

        verify(elasticsearchOperations).get(listingId.toString(), SearchListingDocument.class);
    }

    @Test
    void shouldThrowSearchListingNotFoundExceptionWhenDocumentDoesNotExist() {
        // given
        UUID listingId = UUID.randomUUID();

        when(elasticsearchOperations.get(listingId.toString(), SearchListingDocument.class)).thenReturn(null);

        // when + then
        SearchListingNotFoundException exception = assertThrows(
                SearchListingNotFoundException.class,
                () -> listingSearchService.getById(listingId)
        );

        assertEquals("Listing not found: " + listingId, exception.getMessage());
        verify(elasticsearchOperations).get(listingId.toString(), SearchListingDocument.class);
    }

    @Test
    void shouldThrowSearchListingNotFoundExceptionWhenDocumentExistsButIsNotPublished() {
        // given
        UUID listingId = UUID.randomUUID();

        SearchListingDocument document = SearchListingDocument.builder()
                .id(listingId)
                .status("ARCHIVED")
                .title("Hidden listing")
                .build();

        when(elasticsearchOperations.get(listingId.toString(), SearchListingDocument.class)).thenReturn(document);

        // when + then
        SearchListingNotFoundException exception = assertThrows(
                SearchListingNotFoundException.class,
                () -> listingSearchService.getById(listingId)
        );

        assertEquals("Listing not found: " + listingId, exception.getMessage());
        verify(elasticsearchOperations).get(listingId.toString(), SearchListingDocument.class);
    }

    @Test
    void shouldInvokeElasticsearchSearchTwiceForSearchAndFacets() {
        // given
        when(searchProperties.getDefaultPageSize()).thenReturn(20);

        SearchHits<SearchListingDocument> searchHits = searchHits(List.of(), 0);
        SearchHits<SearchListingDocument> facetHits = facetSearchHits(List.of());

        when(elasticsearchOperations.search(any(Query.class), eq(SearchListingDocument.class)))
                .thenReturn(searchHits, facetHits);

        SearchListingsRequest request = SearchListingsRequest.builder()
                .q("apartment")
                .build();

        // when
        listingSearchService.search(request);

        // then
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(elasticsearchOperations, times(2)).search(queryCaptor.capture(), eq(SearchListingDocument.class));
        assertEquals(2, queryCaptor.getAllValues().size());
    }

    private SearchHits<SearchListingDocument> searchHits(List<SearchListingDocument> documents, long totalHits) {
        @SuppressWarnings("unchecked")
        SearchHits<SearchListingDocument> searchHits = mock(SearchHits.class);

        List<SearchHit<SearchListingDocument>> hits = documents.stream()
                .map(document -> new SearchHit<>(
                        null,
                        document.getId() != null ? document.getId().toString() : UUID.randomUUID().toString(),
                        null,
                        1.0f,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        document
                ))
                .toList();

        doReturn(hits).when(searchHits).getSearchHits();
        doReturn(totalHits).when(searchHits).getTotalHits();

        return searchHits;
    }

    private SearchHits<SearchListingDocument> facetSearchHits(List<SearchListingDocument> documents) {
        @SuppressWarnings("unchecked")
        SearchHits<SearchListingDocument> searchHits = mock(SearchHits.class);

        List<SearchHit<SearchListingDocument>> hits = documents.stream()
                .map(document -> new SearchHit<>(
                        null,
                        document.getId() != null ? document.getId().toString() : UUID.randomUUID().toString(),
                        null,
                        1.0f,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        document
                ))
                .toList();

        doReturn(hits).when(searchHits).getSearchHits();

        return searchHits;
    }
}