package com.rlnkoo.searchservice.domain.service;

import com.rlnkoo.searchservice.api.admin.dto.IndexInfoResponse;
import com.rlnkoo.searchservice.config.SearchProperties;
import com.rlnkoo.searchservice.domain.model.SearchListingDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.query.Query;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndexDiagnosticsServiceTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private SearchProperties searchProperties;

    @Mock
    private IndexOperations indexOperations;

    @InjectMocks
    private IndexDiagnosticsService indexDiagnosticsService;

    @Test
    void shouldReturnIndexInfoWhenIndexExists() {
        // given
        when(searchProperties.getIndexName()).thenReturn("listings");
        when(elasticsearchOperations.indexOps(SearchListingDocument.class)).thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(true);
        when(elasticsearchOperations.count(any(Query.class), eq(SearchListingDocument.class))).thenReturn(15L);

        // when
        IndexInfoResponse response = indexDiagnosticsService.getIndexInfo();

        // then
        assertNotNull(response);
        assertEquals("listings", response.indexName());
        assertTrue(response.exists());
        assertEquals(15L, response.documentCount());

        verify(searchProperties).getIndexName();
        verify(elasticsearchOperations).indexOps(SearchListingDocument.class);
        verify(indexOperations).exists();
        verify(elasticsearchOperations).count(any(Query.class), eq(SearchListingDocument.class));
    }

    @Test
    void shouldReturnZeroDocumentCountWhenIndexDoesNotExist() {
        // given
        when(searchProperties.getIndexName()).thenReturn("listings");
        when(elasticsearchOperations.indexOps(SearchListingDocument.class)).thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(false);

        // when
        IndexInfoResponse response = indexDiagnosticsService.getIndexInfo();

        // then
        assertNotNull(response);
        assertEquals("listings", response.indexName());
        assertFalse(response.exists());
        assertEquals(0L, response.documentCount());

        verify(searchProperties).getIndexName();
        verify(elasticsearchOperations).indexOps(SearchListingDocument.class);
        verify(indexOperations).exists();
        verify(elasticsearchOperations, never()).count(any(Query.class), eq(SearchListingDocument.class));
    }

    @Test
    void shouldUseConfiguredIndexName() {
        // given
        when(searchProperties.getIndexName()).thenReturn("custom-listings-index");
        when(elasticsearchOperations.indexOps(SearchListingDocument.class)).thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(false);

        // when
        IndexInfoResponse response = indexDiagnosticsService.getIndexInfo();

        // then
        assertNotNull(response);
        assertEquals("custom-listings-index", response.indexName());
        assertFalse(response.exists());
        assertEquals(0L, response.documentCount());

        verify(searchProperties).getIndexName();
    }
}