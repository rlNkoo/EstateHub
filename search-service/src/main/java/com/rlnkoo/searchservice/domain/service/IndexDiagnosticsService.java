package com.rlnkoo.searchservice.domain.service;

import com.rlnkoo.searchservice.api.admin.dto.IndexInfoResponse;
import com.rlnkoo.searchservice.config.SearchProperties;
import com.rlnkoo.searchservice.domain.model.SearchListingDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexDiagnosticsService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final SearchProperties searchProperties;

    public IndexInfoResponse getIndexInfo() {
        var indexOps = elasticsearchOperations.indexOps(SearchListingDocument.class);
        String indexName = searchProperties.getIndexName();

        log.info("Index info requested indexName=[{}]", indexName);

        boolean exists = indexOps.exists();
        long documentCount = 0L;

        if (exists) {
            documentCount = elasticsearchOperations.count(
                    org.springframework.data.elasticsearch.client.elc.NativeQuery.builder()
                            .withQuery(q -> q.matchAll(m -> m))
                            .build(),
                    SearchListingDocument.class
            );
        }

        log.info("Index info resolved indexName=[{}] exists=[{}] documentCount=[{}]",
                indexName, exists, documentCount);

        return IndexInfoResponse.builder()
                .indexName(indexName)
                .exists(exists)
                .documentCount(documentCount)
                .build();
    }
}