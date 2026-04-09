package com.rlnkoo.searchservice.api.admin;

import com.rlnkoo.searchservice.api.admin.dto.IndexInfoResponse;
import com.rlnkoo.searchservice.api.admin.dto.ReindexResponse;
import com.rlnkoo.searchservice.domain.exception.ReindexFailedException;
import com.rlnkoo.searchservice.domain.service.IndexDiagnosticsService;
import com.rlnkoo.searchservice.domain.service.ListingReindexService;
import com.rlnkoo.searchservice.persistence.repository.SearchListingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SearchAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListingReindexService listingReindexService;

    @MockitoBean
    private IndexDiagnosticsService indexDiagnosticsService;

    @MockitoBean
    private SearchListingRepository searchListingRepository;

    @Test
    void shouldReturnForbiddenWhenGettingIndexInfoWithoutToken() throws Exception {
        mockMvc.perform(get("/search/admin/index-info"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.path").value("/search/admin/index-info"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturnForbiddenWhenGettingIndexInfoWithNonAdminUser() throws Exception {
        mockMvc.perform(get("/search/admin/index-info"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.path").value("/search/admin/index-info"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnIndexInfoWhenUserHasAdminRole() throws Exception {
        // given
        IndexInfoResponse response = IndexInfoResponse.builder()
                .indexName("listings")
                .exists(true)
                .documentCount(15L)
                .build();

        when(indexDiagnosticsService.getIndexInfo()).thenReturn(response);

        // when + then
        mockMvc.perform(get("/search/admin/index-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indexName").value("listings"))
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.documentCount").value(15));

        verify(indexDiagnosticsService).getIndexInfo();
    }

    @Test
    void shouldReturnUnauthorizedWhenReindexWithoutToken() throws Exception {
        mockMvc.perform(post("/search/admin/reindex"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturnForbiddenWhenReindexWithNonAdminUser() throws Exception {
        mockMvc.perform(post("/search/admin/reindex"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.path").value("/search/admin/reindex"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReindexWhenUserHasAdminRole() throws Exception {
        // given
        ReindexResponse response = ReindexResponse.builder()
                .completed(true)
                .message("Reindex completed successfully")
                .fetchedCount(12)
                .indexedCount(12)
                .failedCount(0)
                .processedPages(2)
                .timestamp(Instant.parse("2025-02-10T10:00:00Z"))
                .build();

        when(listingReindexService.reindexAllPublishedListings()).thenReturn(response);

        // when + then
        mockMvc.perform(post("/search/admin/reindex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.message").value("Reindex completed successfully"))
                .andExpect(jsonPath("$.fetchedCount").value(12))
                .andExpect(jsonPath("$.indexedCount").value(12))
                .andExpect(jsonPath("$.failedCount").value(0))
                .andExpect(jsonPath("$.processedPages").value(2))
                .andExpect(jsonPath("$.timestamp").value("2025-02-10T10:00:00Z"));

        verify(listingReindexService).reindexAllPublishedListings();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnInternalServerErrorWhenReindexFails() throws Exception {
        // given
        when(listingReindexService.reindexAllPublishedListings())
                .thenThrow(new ReindexFailedException(
                        "Failed to reindex published listings",
                        new RuntimeException("boom")
                ));

        // when + then
        mockMvc.perform(post("/search/admin/reindex"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Failed to reindex published listings"))
                .andExpect(jsonPath("$.path").value("/search/admin/reindex"));
    }
}