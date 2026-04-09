package com.rlnkoo.searchservice.integration.listing;

import com.rlnkoo.searchservice.integration.listing.dto.PublishedListingForReindexResponse;
import com.rlnkoo.searchservice.integration.listing.dto.PublishedListingsPageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ListingServiceClientTest {

    private MockRestServiceServer mockServer;
    private ListingServiceClient listingServiceClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://localhost:8082");

        mockServer = MockRestServiceServer.bindTo(builder).build();

        RestClient restClient = builder.build();
        listingServiceClient = new ListingServiceClient(restClient);
    }

    @Test
    void shouldReturnPublishedListingsPageResponse() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();

        String responseBody = """
                {
                  "items": [
                    {
                      "id": "%s",
                      "ownerId": "%s",
                      "status": "PUBLISHED",
                      "version": 3,
                      "publishedAt": "2025-02-01T10:00:00Z",
                      "updatedAt": "2025-02-02T10:00:00Z",
                      "title": "Modern apartment",
                      "description": "Bright apartment in city center",
                      "priceAmount": 650000.00,
                      "currencyCode": "PLN",
                      "address": {
                        "country": "Poland",
                        "city": "Warsaw",
                        "street": "Main Street",
                        "postalCode": "00-001"
                      },
                      "area": 72.50,
                      "rooms": 3,
                      "floor": 4,
                      "propertyType": "APARTMENT",
                      "photoIds": ["%s"]
                    }
                  ],
                  "totalElements": 1,
                  "totalPages": 1,
                  "page": 0,
                  "size": 100
                }
                """.formatted(listingId, ownerId, photoId);

        mockServer.expect(requestTo("http://localhost:8082/admin/listings/published?page=0&size=100"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        // when
        PublishedListingsPageResponse response =
                listingServiceClient.getPublishedListingsForReindex(0, 100, "test-token");

        // then
        assertNotNull(response);
        assertNotNull(response.items());
        assertEquals(1, response.items().size());
        assertEquals(1L, response.totalElements());
        assertEquals(1, response.totalPages());
        assertEquals(0, response.page());
        assertEquals(100, response.size());

        PublishedListingForReindexResponse item = response.items().getFirst();
        assertEquals(listingId, item.id());
        assertEquals(ownerId, item.ownerId());
        assertEquals("PUBLISHED", item.status());
        assertEquals(3, item.version());
        assertEquals("Modern apartment", item.title());
        assertEquals("Bright apartment in city center", item.description());
        assertEquals(0, new BigDecimal("650000.00").compareTo(item.priceAmount()));
        assertEquals("PLN", item.currencyCode());
        assertNotNull(item.address());
        assertEquals("Poland", item.address().country());
        assertEquals("Warsaw", item.address().city());
        assertEquals("Main Street", item.address().street());
        assertEquals("00-001", item.address().postalCode());
        assertEquals(0, new BigDecimal("72.50").compareTo(item.area()));
        assertEquals(3, item.rooms());
        assertEquals(4, item.floor());
        assertEquals("APARTMENT", item.propertyType());
        assertEquals(List.of(photoId), item.photoIds());

        mockServer.verify();
    }

    @Test
    void shouldSendAuthorizationHeaderWithBearerToken() {
        // given
        String responseBody = """
                {
                  "items": [],
                  "totalElements": 0,
                  "totalPages": 0,
                  "page": 1,
                  "size": 50
                }
                """;

        mockServer.expect(requestTo("http://localhost:8082/admin/listings/published?page=1&size=50"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        // when
        PublishedListingsPageResponse response =
                listingServiceClient.getPublishedListingsForReindex(1, 50, "admin-token");

        // then
        assertNotNull(response);
        assertNotNull(response.items());
        assertTrue(response.items().isEmpty());
        assertEquals(0L, response.totalElements());
        assertEquals(0, response.totalPages());
        assertEquals(1, response.page());
        assertEquals(50, response.size());

        mockServer.verify();
    }

    @Test
    void shouldCallPublishedListingsEndpointWithPageAndSize() {
        // given
        String responseBody = """
                {
                  "items": [],
                  "totalElements": 0,
                  "totalPages": 0,
                  "page": 2,
                  "size": 25
                }
                """;

        mockServer.expect(requestTo("http://localhost:8082/admin/listings/published?page=2&size=25"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        // when
        PublishedListingsPageResponse response =
                listingServiceClient.getPublishedListingsForReindex(2, 25, "test-token");

        // then
        assertNotNull(response);
        assertEquals(2, response.page());
        assertEquals(25, response.size());

        mockServer.verify();
    }
}