package com.rlnkoo.listingservice.api.listings;

import com.rlnkoo.listingservice.events.producer.ListingEventsPublisher;
import com.rlnkoo.listingservice.persistence.entity.ListingEntity;
import com.rlnkoo.listingservice.persistence.entity.ListingVersionEntity;
import com.rlnkoo.listingservice.persistence.repository.ListingRepository;
import com.rlnkoo.listingservice.persistence.repository.ListingVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.rlnkoo.listingservice.domain.model.ListingStatus.ARCHIVED;
import static com.rlnkoo.listingservice.domain.model.ListingStatus.DRAFT;
import static com.rlnkoo.listingservice.domain.model.ListingStatus.PUBLISHED;
import static com.rlnkoo.listingservice.domain.model.PropertyType.APARTMENT;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ListingsControllerTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID LISTING_ID_1 = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID LISTING_ID_2 = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID LISTING_ID_3 = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private static final UUID PHOTO_ID_1 = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID PHOTO_ID_2 = UUID.fromString("77777777-7777-7777-7777-777777777777");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ListingVersionRepository listingVersionRepository;

    @MockitoBean
    private ListingEventsPublisher listingEventsPublisher;

    @BeforeEach
    void setUp() {
        clearInvocations(listingEventsPublisher);
        listingVersionRepository.deleteAll();
        listingRepository.deleteAll();
    }

    @Test
    void shouldReturnUnauthorizedWhenCreatingDraftWithoutToken() throws Exception {
        mockMvc.perform(post("/listings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCreateDraftWhenAuthenticated() throws Exception {
        // when + then
        String response = mockMvc.perform(post("/listings")
                        .with(userJwt(USER_ID, "USER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String idValue = response.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
        UUID createdId = UUID.fromString(idValue);

        ListingEntity saved = listingRepository.findById(createdId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(USER_ID, saved.getOwnerId());
        org.junit.jupiter.api.Assertions.assertEquals(DRAFT, saved.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(1, saved.getCurrentVersion());
        org.junit.jupiter.api.Assertions.assertNull(saved.getPublishedVersion());
        org.junit.jupiter.api.Assertions.assertNull(saved.getPublishedAt());
    }

    @Test
    void shouldReturnUnauthorizedWhenUpdatingWithoutToken() throws Exception {
        UUID listingId = UUID.randomUUID();

        mockMvc.perform(put("/listings/{id}", listingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequestJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingMissingListing() throws Exception {
        UUID listingId = UUID.randomUUID();

        mockMvc.perform(put("/listings/{id}", listingId)
                        .with(userJwt(USER_ID, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequestJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Listing not found: " + listingId))
                .andExpect(jsonPath("$.path").value("/listings/" + listingId));
    }

    @Test
    void shouldReturnForbiddenWhenUpdatingForeignListingAsNonAdmin() throws Exception {
        // given
        ListingEntity listing = saveListing(draftListing(LISTING_ID_1, OTHER_USER_ID, 1));

        // when + then
        mockMvc.perform(put("/listings/{id}", listing.getId())
                        .with(userJwt(USER_ID, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequestJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied for listing: " + listing.getId()))
                .andExpect(jsonPath("$.path").value("/listings/" + listing.getId()));
    }

    @Test
    void shouldUpdateListingWhenOwnerIsAuthenticated() throws Exception {
        // given
        ListingEntity listing = saveListing(draftListing(LISTING_ID_1, USER_ID, 1));

        // when + then
        mockMvc.perform(put("/listings/{id}", listing.getId())
                        .with(userJwt(USER_ID, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(listing.getId().toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.version").value(2));

        ListingEntity updated = listingRepository.findById(listing.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(2, updated.getCurrentVersion());
        org.junit.jupiter.api.Assertions.assertEquals(DRAFT, updated.getStatus());

        ListingVersionEntity version = listingVersionRepository
                .findByListingIdAndVersionNo(listing.getId(), 2)
                .orElseThrow();

        org.junit.jupiter.api.Assertions.assertEquals("Modern apartment in city center", version.getTitle());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("550000.00"), version.getPriceAmount());
        org.junit.jupiter.api.Assertions.assertEquals("PLN", version.getCurrencyCode());
        org.junit.jupiter.api.Assertions.assertEquals(APARTMENT, version.getPropertyType());
        org.junit.jupiter.api.Assertions.assertEquals(List.of(PHOTO_ID_1, PHOTO_ID_2), version.getPhotoIds());

        verify(listingEventsPublisher, never()).publishListingUpdated(any(), any());
    }

    @Test
    void shouldReturnConflictWhenUpdatingArchivedListing() throws Exception {
        // given
        ListingEntity listing = saveListing(archivedListing(LISTING_ID_1, USER_ID, 2));

        // when + then
        mockMvc.perform(put("/listings/{id}", listing.getId())
                        .with(userJwt(USER_ID, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequestJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Listing is not editable in status ARCHIVED (id=" + listing.getId() + ")"))
                .andExpect(jsonPath("$.path").value("/listings/" + listing.getId()));
    }

    @Test
    void shouldReturnBadRequestWhenUpdateRequestIsInvalid() throws Exception {
        // given
        ListingEntity listing = saveListing(draftListing(LISTING_ID_1, USER_ID, 1));

        String invalidBody = """
                {
                  "title": "",
                  "description": "Nice flat",
                  "priceAmount": -1,
                  "currencyCode": "PL",
                  "address": {
                    "country": "",
                    "city": "",
                    "street": "Main Street 1",
                    "postalCode": "00-001"
                  },
                  "area": 0,
                  "rooms": 0,
                  "floor": 500,
                  "propertyType": "",
                  "photoIds": []
                }
                """;

        // when + then
        mockMvc.perform(put("/listings/{id}", listing.getId())
                        .with(userJwt(USER_ID, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/listings/" + listing.getId()));
    }

    @Test
    void shouldReturnBadRequestWhenPropertyTypeIsInvalid() throws Exception {
        // given
        ListingEntity listing = saveListing(draftListing(LISTING_ID_1, USER_ID, 1));

        // when + then
        mockMvc.perform(put("/listings/{id}", listing.getId())
                        .with(userJwt(USER_ID, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequestJsonWithPropertyType("CASTLE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid propertyType: CASTLE"))
                .andExpect(jsonPath("$.path").value("/listings/" + listing.getId()));
    }

    @Test
    void shouldPublishListingUpdatedEventWhenUpdatingPublishedListing() throws Exception {
        // given
        ListingEntity listing = saveListing(publishedListing(LISTING_ID_1, USER_ID, 2, 2));
        saveVersion(publishableVersion(listing.getId(), 2));

        // when + then
        mockMvc.perform(put("/listings/{id}", listing.getId())
                        .with(userJwt(USER_ID, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(listing.getId().toString()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.version").value(3));

        ListingEntity updated = listingRepository.findById(listing.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(3, updated.getCurrentVersion());
        org.junit.jupiter.api.Assertions.assertEquals(3, updated.getPublishedVersion());

        verify(listingEventsPublisher).publishListingUpdated(any(), any());
    }

    @Test
    void shouldReturnUnauthorizedWhenPublishingWithoutToken() throws Exception {
        UUID listingId = UUID.randomUUID();

        mockMvc.perform(post("/listings/{id}/publish", listingId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldPublishDraftListingWhenOwnerIsAuthenticated() throws Exception {
        // given
        ListingEntity listing = saveListing(draftListing(LISTING_ID_1, USER_ID, 2));
        saveVersion(publishableVersion(listing.getId(), 2));

        // when + then
        mockMvc.perform(post("/listings/{id}/publish", listing.getId())
                        .with(userJwt(USER_ID, "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(listing.getId().toString()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.version").value(2));

        ListingEntity updated = listingRepository.findById(listing.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(PUBLISHED, updated.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(2, updated.getPublishedVersion());
        org.junit.jupiter.api.Assertions.assertNotNull(updated.getPublishedAt());

        verify(listingEventsPublisher).publishListingPublished(any(), any());
    }

    @Test
    void shouldReturnConflictWhenPublishingWithoutContent() throws Exception {
        // given
        ListingEntity listing = saveListing(draftListing(LISTING_ID_1, USER_ID, 2));

        // when + then
        mockMvc.perform(post("/listings/{id}/publish", listing.getId())
                        .with(userJwt(USER_ID, "USER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Listing content not found for listing: " + listing.getId()))
                .andExpect(jsonPath("$.path").value("/listings/" + listing.getId() + "/publish"));
    }

    @Test
    void shouldReturnConflictWhenListingIsNotPublishable() throws Exception {
        // given
        ListingEntity listing = saveListing(draftListing(LISTING_ID_1, USER_ID, 2));
        saveVersion(nonPublishableVersionMissingTitle(listing.getId(), 2));

        // when + then
        mockMvc.perform(post("/listings/{id}/publish", listing.getId())
                        .with(userJwt(USER_ID, "USER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Listing cannot be published (id=" + listing.getId() + "): title missing"))
                .andExpect(jsonPath("$.path").value("/listings/" + listing.getId() + "/publish"));
    }

    @Test
    void shouldReturnUnauthorizedWhenArchivingWithoutToken() throws Exception {
        UUID listingId = UUID.randomUUID();

        mockMvc.perform(post("/listings/{id}/archive", listingId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldArchivePublishedListing() throws Exception {
        // given
        ListingEntity listing = saveListing(publishedListing(LISTING_ID_1, USER_ID, 3, 3));

        // when + then
        mockMvc.perform(post("/listings/{id}/archive", listing.getId())
                        .with(userJwt(USER_ID, "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(listing.getId().toString()))
                .andExpect(jsonPath("$.status").value("ARCHIVED"))
                .andExpect(jsonPath("$.version").value(3));

        ListingEntity updated = listingRepository.findById(listing.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(ARCHIVED, updated.getStatus());

        verify(listingEventsPublisher).publishListingArchived(any(), any());
    }

    @Test
    void shouldReturnConflictWhenArchivingDraftListing() throws Exception {
        // given
        ListingEntity listing = saveListing(draftListing(LISTING_ID_1, USER_ID, 1));

        // when + then
        mockMvc.perform(post("/listings/{id}/archive", listing.getId())
                        .with(userJwt(USER_ID, "USER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid listing status transition for " + listing.getId() + ": DRAFT -> ARCHIVED"))
                .andExpect(jsonPath("$.path").value("/listings/" + listing.getId() + "/archive"));
    }

    @Test
    void shouldReturnPublishedListingForAnonymousUser() throws Exception {
        // given
        ListingEntity listing = saveListing(publishedListing(LISTING_ID_1, USER_ID, 3, 2));
        saveVersion(publishableVersion(listing.getId(), 2));

        // when + then
        mockMvc.perform(get("/listings/{id}", listing.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(listing.getId().toString()))
                .andExpect(jsonPath("$.ownerId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.title").value("Beautiful apartment"))
                .andExpect(jsonPath("$.priceAmount").value(700000.00))
                .andExpect(jsonPath("$.currencyCode").value("PLN"))
                .andExpect(jsonPath("$.propertyType").value("APARTMENT"))
                .andExpect(jsonPath("$.photoIds", containsInAnyOrder(
                        PHOTO_ID_1.toString(),
                        PHOTO_ID_2.toString()
                )));
    }

    @Test
    void shouldReturnNotFoundForAnonymousUserWhenListingIsDraft() throws Exception {
        // given
        ListingEntity listing = saveListing(draftListing(LISTING_ID_1, USER_ID, 2));
        saveVersion(publishableVersion(listing.getId(), 2));

        // when + then
        mockMvc.perform(get("/listings/{id}", listing.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Listing not found: " + listing.getId()))
                .andExpect(jsonPath("$.path").value("/listings/" + listing.getId()));
    }

    @Test
    void shouldReturnDraftListingForOwner() throws Exception {
        // given
        ListingEntity listing = saveListing(draftListing(LISTING_ID_1, USER_ID, 2));
        saveVersion(publishableVersion(listing.getId(), 2));

        // when + then
        mockMvc.perform(get("/listings/{id}", listing.getId())
                        .with(userJwt(USER_ID, "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(listing.getId().toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.title").value("Beautiful apartment"));
    }

    @Test
    void shouldReturnDraftListingForAdmin() throws Exception {
        // given
        ListingEntity listing = saveListing(draftListing(LISTING_ID_1, OTHER_USER_ID, 2));
        saveVersion(publishableVersion(listing.getId(), 2));

        // when + then
        mockMvc.perform(get("/listings/{id}", listing.getId())
                        .with(userJwt(USER_ID, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(listing.getId().toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void shouldReturnConflictWhenListingVersionContentMissing() throws Exception {
        // given
        ListingEntity listing = saveListing(publishedListing(LISTING_ID_1, USER_ID, 3, 2));

        // when + then
        mockMvc.perform(get("/listings/{id}", listing.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Listing content not found for listing: " + listing.getId()))
                .andExpect(jsonPath("$.path").value("/listings/" + listing.getId()));
    }

    @Test
    void shouldReturnUnauthorizedWhenGettingMyListingsWithoutToken() throws Exception {
        mockMvc.perform(get("/listings/mine"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnOnlyCurrentUsersListings() throws Exception {
        // given
        saveListing(draftListing(LISTING_ID_1, USER_ID, 1));
        saveListing(publishedListing(LISTING_ID_2, USER_ID, 2, 2));
        saveListing(draftListing(LISTING_ID_3, OTHER_USER_ID, 3));

        // when + then
        mockMvc.perform(get("/listings/mine")
                        .with(userJwt(USER_ID, "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(
                        LISTING_ID_1.toString(),
                        LISTING_ID_2.toString()
                )));
    }

    private RequestPostProcessor userJwt(UUID userId, String... roles) {
        List<org.springframework.security.core.GrantedAuthority> authorities =
                java.util.Arrays.stream(roles)
                        .map(role -> (org.springframework.security.core.GrantedAuthority)
                                new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();

        return jwt().jwt(jwt -> jwt
                        .subject(userId.toString())
                        .claim("email", "user@example.com")
                        .claim("roles", List.of(roles)))
                .authorities(authorities);
    }

    private String validUpdateRequestJson() {
        return """
                {
                  "title": "Modern apartment in city center",
                  "description": "Nice flat for sale",
                  "priceAmount": 550000.00,
                  "currencyCode": "PLN",
                  "address": {
                    "country": "Poland",
                    "city": "Warsaw",
                    "street": "Main Street 1",
                    "postalCode": "00-001"
                  },
                  "area": 52.50,
                  "rooms": 3,
                  "floor": 4,
                  "propertyType": "APARTMENT",
                  "photoIds": [
                    "%s",
                    "%s"
                  ]
                }
                """.formatted(PHOTO_ID_1, PHOTO_ID_2);
    }

    private String validUpdateRequestJsonWithPropertyType(String propertyType) {
        return """
                {
                  "title": "Modern apartment in city center",
                  "description": "Nice flat for sale",
                  "priceAmount": 550000.00,
                  "currencyCode": "PLN",
                  "address": {
                    "country": "Poland",
                    "city": "Warsaw",
                    "street": "Main Street 1",
                    "postalCode": "00-001"
                  },
                  "area": 52.50,
                  "rooms": 3,
                  "floor": 4,
                  "propertyType": "%s",
                  "photoIds": [
                    "%s"
                  ]
                }
                """.formatted(propertyType, PHOTO_ID_1);
    }

    private ListingEntity saveListing(ListingEntity listing) {
        return listingRepository.save(listing);
    }

    private ListingVersionEntity saveVersion(ListingVersionEntity version) {
        return listingVersionRepository.save(version);
    }

    private ListingEntity draftListing(UUID id, UUID ownerId, int currentVersion) {
        return ListingEntity.builder()
                .id(id)
                .ownerId(ownerId)
                .status(DRAFT)
                .currentVersion(currentVersion)
                .publishedVersion(null)
                .build();
    }

    private ListingEntity publishedListing(UUID id, UUID ownerId, int currentVersion, Integer publishedVersion) {
        return ListingEntity.builder()
                .id(id)
                .ownerId(ownerId)
                .status(PUBLISHED)
                .currentVersion(currentVersion)
                .publishedVersion(publishedVersion)
                .publishedAt(Instant.now())
                .build();
    }

    private ListingEntity archivedListing(UUID id, UUID ownerId, int currentVersion) {
        return ListingEntity.builder()
                .id(id)
                .ownerId(ownerId)
                .status(ARCHIVED)
                .currentVersion(currentVersion)
                .publishedVersion(currentVersion)
                .publishedAt(Instant.now())
                .build();
    }

    private ListingVersionEntity publishableVersion(UUID listingId, int versionNo) {
        return ListingVersionEntity.builder()
                .id(UUID.randomUUID())
                .listingId(listingId)
                .versionNo(versionNo)
                .title("Beautiful apartment")
                .description("Spacious and sunny")
                .priceAmount(new BigDecimal("700000.00"))
                .currencyCode("PLN")
                .address(ListingVersionEntity.AddressEmbeddable.builder()
                        .country("Poland")
                        .city("Krakow")
                        .street("Sunny Street 15")
                        .postalCode("30-001")
                        .build())
                .area(new BigDecimal("68.00"))
                .rooms(4)
                .floor(2)
                .propertyType(APARTMENT)
                .photoIds(List.of(PHOTO_ID_1, PHOTO_ID_2))
                .build();
    }

    private ListingVersionEntity nonPublishableVersionMissingTitle(UUID listingId, int versionNo) {
        return ListingVersionEntity.builder()
                .id(UUID.randomUUID())
                .listingId(listingId)
                .versionNo(versionNo)
                .title(" ")
                .description("Spacious and sunny")
                .priceAmount(new BigDecimal("700000.00"))
                .currencyCode("PLN")
                .address(ListingVersionEntity.AddressEmbeddable.builder()
                        .country("Poland")
                        .city("Krakow")
                        .street("Sunny Street 15")
                        .postalCode("30-001")
                        .build())
                .area(new BigDecimal("68.00"))
                .rooms(4)
                .floor(2)
                .propertyType(APARTMENT)
                .photoIds(List.of(PHOTO_ID_1))
                .build();
    }
}