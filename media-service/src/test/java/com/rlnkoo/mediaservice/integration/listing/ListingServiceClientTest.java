package com.rlnkoo.mediaservice.integration.listing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rlnkoo.mediaservice.domain.exception.ExternalServiceException;
import com.rlnkoo.mediaservice.domain.exception.ListingNotFoundException;
import com.rlnkoo.mediaservice.integration.listing.dto.ListingDetailsDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingServiceClientTest {

    @Mock
    private RestClient restClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ListingServiceClient listingServiceClient;

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void shouldReturnListingDetailsWhenRequestIsSuccessful() throws Exception {
        // given
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        String json = """
                {
                  "id": "%s",
                  "ownerId": "%s",
                  "status": "PUBLISHED"
                }
                """.formatted(listingId, ownerId);

        ListingDetailsDto dto = new ListingDetailsDto(listingId, ownerId, "PUBLISHED");

        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("/listings/{id}", listingId)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class)).thenReturn(ResponseEntity.ok(json));
        when(objectMapper.readValue(json, ListingDetailsDto.class)).thenReturn(dto);

        // when
        ListingDetailsDto result = listingServiceClient.getListing(listingId);

        // then
        assertEquals(dto, result);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void shouldThrowListingNotFoundExceptionWhen404Returned() {
        // given
        UUID listingId = UUID.randomUUID();

        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("/listings/{id}", listingId)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        RestClientResponseException exception = new RestClientResponseException(
                "Not found",
                404,
                "Not Found",
                null,
                null,
                StandardCharsets.UTF_8
        );

        when(responseSpec.toEntity(String.class)).thenThrow(exception);

        // when + then
        ListingNotFoundException ex = assertThrows(
                ListingNotFoundException.class,
                () -> listingServiceClient.getListing(listingId)
        );

        assertEquals("Listing not found: " + listingId, ex.getMessage());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void shouldThrowExternalServiceExceptionForNon404Errors() {
        // given
        UUID listingId = UUID.randomUUID();

        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("/listings/{id}", listingId)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        RestClientResponseException exception = new RestClientResponseException(
                "Internal error",
                500,
                "Internal Server Error",
                null,
                null,
                StandardCharsets.UTF_8
        );

        when(responseSpec.toEntity(String.class)).thenThrow(exception);

        // when + then
        ExternalServiceException ex = assertThrows(
                ExternalServiceException.class,
                () -> listingServiceClient.getListing(listingId)
        );

        assertTrue(ex.getMessage().contains("listing-service error"));
        assertTrue(ex.getMessage().contains("HTTP 500"));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void shouldThrowExternalServiceExceptionWhenJsonParsingFails() throws Exception {
        // given
        UUID listingId = UUID.randomUUID();
        String json = "{ invalid json }";

        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("/listings/{id}", listingId)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class)).thenReturn(ResponseEntity.ok(json));
        when(objectMapper.readValue(json, ListingDetailsDto.class))
                .thenThrow(new RuntimeException("JSON parse error"));

        // when + then
        ExternalServiceException ex = assertThrows(
                ExternalServiceException.class,
                () -> listingServiceClient.getListing(listingId)
        );

        assertTrue(ex.getMessage().contains("Unexpected error"));
        assertTrue(ex.getMessage().contains("JSON parse error"));
    }
}