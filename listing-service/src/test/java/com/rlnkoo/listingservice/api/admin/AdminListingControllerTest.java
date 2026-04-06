package com.rlnkoo.listingservice.api.admin;

import com.rlnkoo.listingservice.persistence.entity.ListingEntity;
import com.rlnkoo.listingservice.persistence.entity.ListingVersionEntity;
import com.rlnkoo.listingservice.persistence.repository.ListingRepository;
import com.rlnkoo.listingservice.persistence.repository.ListingVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.rlnkoo.listingservice.domain.model.ListingStatus.DRAFT;
import static com.rlnkoo.listingservice.domain.model.ListingStatus.PUBLISHED;
import static com.rlnkoo.listingservice.domain.model.PropertyType.APARTMENT;
import static com.rlnkoo.listingservice.domain.model.PropertyType.HOUSE;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminListingControllerTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID LISTING_ID_1 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID LISTING_ID_2 = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID LISTING_ID_3 = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private static final UUID OWNER_ID_1 = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID OWNER_ID_2 = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID OWNER_ID_3 = UUID.fromString("77777777-7777-7777-7777-777777777777");

    private static final UUID PHOTO_ID_1 = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private static final UUID PHOTO_ID_2 = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID PHOTO_ID_3 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final Instant PUBLISHED_AT_1 = Instant.parse("2025-01-10T10:15:30Z");
    private static final Instant PUBLISHED_AT_2 = Instant.parse("2025-01-09T08:00:00Z");
    private static final Instant UPDATED_AT_1 = Instant.parse("2025-01-11T12:00:00Z");
    private static final Instant UPDATED_AT_2 = Instant.parse("2025-01-10T15:30:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ListingVersionRepository listingVersionRepository;

    @BeforeEach
    void setUp() {
        listingVersionRepository.deleteAll();
        listingRepository.deleteAll();
    }

    @Test
    void shouldReturnUnauthorizedWhenGettingPublishedListingsWithoutToken() throws Exception {
        mockMvc.perform(get("/admin/listings/published"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnForbiddenWhenAuthenticatedUserIsNotAdmin() throws Exception {
        mockMvc.perform(get("/admin/listings/published")
                        .with(userJwt(USER_ID, "USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.path").value("/admin/listings/published"));
    }

    @Test
    void shouldReturnPublishedListingsPageWhenAuthenticatedUserIsAdmin() throws Exception {
        // given
        ListingEntity listing1 = saveListing(
                publishedListing(LISTING_ID_1, OWNER_ID_1, 4, 3, PUBLISHED_AT_1, UPDATED_AT_1)
        );
        ListingEntity listing2 = saveListing(
                publishedListing(LISTING_ID_2, OWNER_ID_2, 2, 2, PUBLISHED_AT_2, UPDATED_AT_2)
        );

        saveVersion(publishedVersion(
                listing1.getId(),
                3,
                "Luxury apartment in city center",
                "Spacious apartment with balcony",
                new BigDecimal("950000.00"),
                "PLN",
                "Poland",
                "Warsaw",
                "Main Street 15",
                "00-100",
                new BigDecimal("82.50"),
                4,
                6,
                APARTMENT,
                List.of(PHOTO_ID_1, PHOTO_ID_2)
        ));

        saveVersion(publishedVersion(
                listing2.getId(),
                2,
                "House with garden",
                "Detached house in quiet neighborhood",
                new BigDecimal("1250000.00"),
                "PLN",
                "Poland",
                "Krakow",
                "Garden Street 7",
                "30-200",
                new BigDecimal("140.00"),
                5,
                1,
                HOUSE,
                List.of(PHOTO_ID_3)
        ));

        // extra listing that should not be included
        saveListing(draftListing(LISTING_ID_3, OWNER_ID_3, 1));

        // when + then
        mockMvc.perform(get("/admin/listings/published")
                        .with(userJwt(USER_ID, "ADMIN"))
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.items[*].id", containsInAnyOrder(
                        LISTING_ID_1.toString(),
                        LISTING_ID_2.toString()
                )))
                .andExpect(jsonPath("$.items[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.items[1].status").value("PUBLISHED"));
    }

    @Test
    void shouldSkipPublishedListingWithoutPublishedVersion() throws Exception {
        // given
        saveListing(ListingEntity.builder()
                .id(LISTING_ID_1)
                .ownerId(OWNER_ID_1)
                .status(PUBLISHED)
                .currentVersion(4)
                .publishedVersion(null)
                .createdAt(Instant.now())
                .updatedAt(UPDATED_AT_1)
                .publishedAt(PUBLISHED_AT_1)
                .build());

        // when + then
        mockMvc.perform(get("/admin/listings/published")
                        .with(userJwt(USER_ID, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldSkipPublishedListingWhenPublishedVersionContentIsMissing() throws Exception {
        // given
        saveListing(
                publishedListing(LISTING_ID_1, OWNER_ID_1, 4, 3, PUBLISHED_AT_1, UPDATED_AT_1)
        );

        // when + then
        mockMvc.perform(get("/admin/listings/published")
                        .with(userJwt(USER_ID, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldReturnBadRequestWhenPageIsNegative() throws Exception {
        mockMvc.perform(get("/admin/listings/published")
                        .with(userJwt(USER_ID, "ADMIN"))
                        .param("page", "-1")
                        .param("size", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("page must be greater than or equal to 0"))
                .andExpect(jsonPath("$.path").value("/admin/listings/published"));
    }

    @Test
    void shouldReturnBadRequestWhenSizeIsLessThanOne() throws Exception {
        mockMvc.perform(get("/admin/listings/published")
                        .with(userJwt(USER_ID, "ADMIN"))
                        .param("page", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("size must be greater than 0"))
                .andExpect(jsonPath("$.path").value("/admin/listings/published"));
    }

    @Test
    void shouldReturnBadRequestWhenSizeIsGreaterThanFiveHundred() throws Exception {
        mockMvc.perform(get("/admin/listings/published")
                        .with(userJwt(USER_ID, "ADMIN"))
                        .param("page", "0")
                        .param("size", "501"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("size must be less than or equal to 500"))
                .andExpect(jsonPath("$.path").value("/admin/listings/published"));
    }

    private RequestPostProcessor userJwt(UUID userId, String... roles) {
        SimpleGrantedAuthority[] authorities = java.util.Arrays.stream(roles)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toArray(SimpleGrantedAuthority[]::new);

        return jwt().jwt(jwt -> jwt
                        .subject(userId.toString())
                        .claim("email", "admin@example.com")
                        .claim("roles", List.of(roles)))
                .authorities(authorities);
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

    private ListingEntity publishedListing(
            UUID id,
            UUID ownerId,
            int currentVersion,
            Integer publishedVersion,
            Instant publishedAt,
            Instant updatedAt
    ) {
        return ListingEntity.builder()
                .id(id)
                .ownerId(ownerId)
                .status(PUBLISHED)
                .currentVersion(currentVersion)
                .publishedVersion(publishedVersion)
                .publishedAt(publishedAt)
                .updatedAt(updatedAt)
                .build();
    }

    private ListingVersionEntity publishedVersion(
            UUID listingId,
            int versionNo,
            String title,
            String description,
            BigDecimal priceAmount,
            String currencyCode,
            String country,
            String city,
            String street,
            String postalCode,
            BigDecimal area,
            Integer rooms,
            Integer floor,
            com.rlnkoo.listingservice.domain.model.PropertyType propertyType,
            List<UUID> photoIds
    ) {
        return ListingVersionEntity.builder()
                .id(UUID.randomUUID())
                .listingId(listingId)
                .versionNo(versionNo)
                .title(title)
                .description(description)
                .priceAmount(priceAmount)
                .currencyCode(currencyCode)
                .address(ListingVersionEntity.AddressEmbeddable.builder()
                        .country(country)
                        .city(city)
                        .street(street)
                        .postalCode(postalCode)
                        .build())
                .area(area)
                .rooms(rooms)
                .floor(floor)
                .propertyType(propertyType)
                .photoIds(photoIds)
                .build();
    }
}