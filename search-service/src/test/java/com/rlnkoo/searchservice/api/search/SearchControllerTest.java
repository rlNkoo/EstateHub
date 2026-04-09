package com.rlnkoo.searchservice.api.search;

import com.rlnkoo.searchservice.api.search.dto.SearchFacetBucketResponse;
import com.rlnkoo.searchservice.api.search.dto.SearchFacetsResponse;
import com.rlnkoo.searchservice.api.search.dto.SearchListingItemResponse;
import com.rlnkoo.searchservice.api.search.dto.SearchListingsRequest;
import com.rlnkoo.searchservice.api.search.dto.SearchListingsResponse;
import com.rlnkoo.searchservice.domain.exception.InvalidSearchRequestException;
import com.rlnkoo.searchservice.domain.exception.SearchListingNotFoundException;
import com.rlnkoo.searchservice.domain.service.ListingSearchService;
import com.rlnkoo.searchservice.persistence.repository.SearchListingRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListingSearchService listingSearchService;

    @MockitoBean
    private SearchListingRepository searchListingRepository;

    @Test
    void shouldReturnSearchResultsForPublicEndpointWithoutAuthentication() throws Exception {
        // given
        UUID listingId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();

        SearchListingsResponse response = SearchListingsResponse.builder()
                .items(List.of(
                        SearchListingItemResponse.builder()
                                .id(listingId)
                                .title("Modern apartment")
                                .description("Bright apartment in city center")
                                .priceAmount("650000")
                                .currencyCode("PLN")
                                .country("Poland")
                                .city("Warsaw")
                                .street("Main Street")
                                .postalCode("00-001")
                                .area(new BigDecimal("72.50"))
                                .rooms(3)
                                .floor(4)
                                .propertyType("APARTMENT")
                                .photoIds(List.of(photoId))
                                .publishedAt(Instant.parse("2025-02-01T10:00:00Z"))
                                .build()
                ))
                .totalElements(1)
                .totalPages(1)
                .page(0)
                .size(20)
                .sort("publishedAt,desc")
                .facets(SearchFacetsResponse.builder()
                        .cities(List.of(
                                SearchFacetBucketResponse.builder().value("Warsaw").count(1).build()
                        ))
                        .propertyTypes(List.of(
                                SearchFacetBucketResponse.builder().value("APARTMENT").count(1).build()
                        ))
                        .rooms(List.of(
                                SearchFacetBucketResponse.builder().value("3").count(1).build()
                        ))
                        .build())
                .build();

        when(listingSearchService.search(any(SearchListingsRequest.class))).thenReturn(response);

        // when + then
        mockMvc.perform(get("/search/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(listingId.toString()))
                .andExpect(jsonPath("$.items[0].title").value("Modern apartment"))
                .andExpect(jsonPath("$.items[0].description").value("Bright apartment in city center"))
                .andExpect(jsonPath("$.items[0].priceAmount").value("650000"))
                .andExpect(jsonPath("$.items[0].currencyCode").value("PLN"))
                .andExpect(jsonPath("$.items[0].country").value("Poland"))
                .andExpect(jsonPath("$.items[0].city").value("Warsaw"))
                .andExpect(jsonPath("$.items[0].street").value("Main Street"))
                .andExpect(jsonPath("$.items[0].postalCode").value("00-001"))
                .andExpect(jsonPath("$.items[0].area").value(72.50))
                .andExpect(jsonPath("$.items[0].rooms").value(3))
                .andExpect(jsonPath("$.items[0].floor").value(4))
                .andExpect(jsonPath("$.items[0].propertyType").value("APARTMENT"))
                .andExpect(jsonPath("$.items[0].photoIds[0]").value(photoId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.sort").value("publishedAt,desc"))
                .andExpect(jsonPath("$.facets.cities[0].value").value("Warsaw"))
                .andExpect(jsonPath("$.facets.cities[0].count").value(1))
                .andExpect(jsonPath("$.facets.propertyTypes[0].value").value("APARTMENT"))
                .andExpect(jsonPath("$.facets.propertyTypes[0].count").value(1))
                .andExpect(jsonPath("$.facets.rooms[0].value").value("3"))
                .andExpect(jsonPath("$.facets.rooms[0].count").value(1));

        verify(listingSearchService).search(any(SearchListingsRequest.class));
    }

    @Test
    void shouldPassAllQueryParametersToSearchService() throws Exception {
        // given
        SearchListingsResponse response = SearchListingsResponse.builder()
                .items(List.of())
                .totalElements(0)
                .totalPages(0)
                .page(1)
                .size(10)
                .sort("priceAmount,asc")
                .facets(SearchFacetsResponse.builder()
                        .cities(List.of())
                        .propertyTypes(List.of())
                        .rooms(List.of())
                        .build())
                .build();

        when(listingSearchService.search(any(SearchListingsRequest.class))).thenReturn(response);

        // when
        mockMvc.perform(get("/search/listings")
                        .param("q", "apartment")
                        .param("city", "Warsaw")
                        .param("country", "Poland")
                        .param("propertyType", "APARTMENT")
                        .param("priceFrom", "100000")
                        .param("priceTo", "1000000")
                        .param("areaFrom", "20")
                        .param("areaTo", "150")
                        .param("rooms", "3")
                        .param("floor", "4")
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "priceAmount,asc"))
                .andExpect(status().isOk());

        // then
        ArgumentCaptor<SearchListingsRequest> captor = ArgumentCaptor.forClass(SearchListingsRequest.class);
        verify(listingSearchService).search(captor.capture());

        SearchListingsRequest request = captor.getValue();
        assertEquals("apartment", request.q());
        assertEquals("Warsaw", request.city());
        assertEquals("Poland", request.country());
        assertEquals("APARTMENT", request.propertyType());
        assertEquals(new BigDecimal("100000"), request.priceFrom());
        assertEquals(new BigDecimal("1000000"), request.priceTo());
        assertEquals(new BigDecimal("20"), request.areaFrom());
        assertEquals(new BigDecimal("150"), request.areaTo());
        assertEquals(3, request.rooms());
        assertEquals(4, request.floor());
        assertEquals(1, request.page());
        assertEquals(10, request.size());
        assertEquals("priceAmount,asc", request.sort());
    }

    @Test
    void shouldPassNullQueryParametersWhenTheyAreNotProvided() throws Exception {
        // given
        SearchListingsResponse response = SearchListingsResponse.builder()
                .items(List.of())
                .totalElements(0)
                .totalPages(0)
                .page(0)
                .size(20)
                .sort("publishedAt,desc")
                .facets(SearchFacetsResponse.builder()
                        .cities(List.of())
                        .propertyTypes(List.of())
                        .rooms(List.of())
                        .build())
                .build();

        when(listingSearchService.search(any(SearchListingsRequest.class))).thenReturn(response);

        // when
        mockMvc.perform(get("/search/listings"))
                .andExpect(status().isOk());

        // then
        ArgumentCaptor<SearchListingsRequest> captor = ArgumentCaptor.forClass(SearchListingsRequest.class);
        verify(listingSearchService).search(captor.capture());

        SearchListingsRequest request = captor.getValue();
        assertNull(request.q());
        assertNull(request.city());
        assertNull(request.country());
        assertNull(request.propertyType());
        assertNull(request.priceFrom());
        assertNull(request.priceTo());
        assertNull(request.areaFrom());
        assertNull(request.areaTo());
        assertNull(request.rooms());
        assertNull(request.floor());
        assertNull(request.page());
        assertNull(request.size());
        assertNull(request.sort());
    }

    @Test
    void shouldReturnBadRequestWhenSearchRequestIsInvalid() throws Exception {
        // given
        when(listingSearchService.search(any(SearchListingsRequest.class)))
                .thenThrow(new InvalidSearchRequestException("size must be less than or equal to 100"));

        // when + then
        mockMvc.perform(get("/search/listings")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("size must be less than or equal to 100"))
                .andExpect(jsonPath("$.path").value("/search/listings"));
    }

    @Test
    void shouldReturnListingByIdForPublicEndpointWithoutAuthentication() throws Exception {
        // given
        UUID listingId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();

        SearchListingItemResponse response = SearchListingItemResponse.builder()
                .id(listingId)
                .title("Premium apartment")
                .description("Great location")
                .priceAmount("700000")
                .currencyCode("PLN")
                .country("Poland")
                .city("Warsaw")
                .street("Marszalkowska")
                .postalCode("00-001")
                .area(new BigDecimal("70.00"))
                .rooms(3)
                .floor(5)
                .propertyType("APARTMENT")
                .photoIds(List.of(photoId))
                .publishedAt(Instant.parse("2025-01-15T12:00:00Z"))
                .build();

        when(listingSearchService.getById(listingId)).thenReturn(response);

        // when + then
        mockMvc.perform(get("/search/listings/{listingId}", listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(listingId.toString()))
                .andExpect(jsonPath("$.title").value("Premium apartment"))
                .andExpect(jsonPath("$.description").value("Great location"))
                .andExpect(jsonPath("$.priceAmount").value("700000"))
                .andExpect(jsonPath("$.currencyCode").value("PLN"))
                .andExpect(jsonPath("$.country").value("Poland"))
                .andExpect(jsonPath("$.city").value("Warsaw"))
                .andExpect(jsonPath("$.street").value("Marszalkowska"))
                .andExpect(jsonPath("$.postalCode").value("00-001"))
                .andExpect(jsonPath("$.area").value(70.00))
                .andExpect(jsonPath("$.rooms").value(3))
                .andExpect(jsonPath("$.floor").value(5))
                .andExpect(jsonPath("$.propertyType").value("APARTMENT"))
                .andExpect(jsonPath("$.photoIds[0]").value(photoId.toString()));

        verify(listingSearchService).getById(listingId);
    }

    @Test
    void shouldReturnNotFoundWhenListingDoesNotExist() throws Exception {
        // given
        UUID listingId = UUID.randomUUID();

        when(listingSearchService.getById(listingId))
                .thenThrow(new SearchListingNotFoundException(listingId));

        // when + then
        mockMvc.perform(get("/search/listings/{listingId}", listingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Listing not found: " + listingId))
                .andExpect(jsonPath("$.path").value("/search/listings/" + listingId));
    }

    @Test
    void shouldReturnBadRequestWhenListingIdIsInvalidUuid() throws Exception {
        // when + then
        mockMvc.perform(get("/search/listings/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/search/listings/not-a-uuid"));

        verify(listingSearchService, never()).getById(any());
    }
}