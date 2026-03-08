package com.rlnkoo.searchservice.config;

import com.rlnkoo.searchservice.domain.model.SearchListingDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.IndexOperations;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ElasticsearchIndexInitializer {

    private final ElasticsearchOperations elasticsearchOperations;

    @Bean
    ApplicationRunner initializeSearchIndex() {
        return args -> {

            IndexOperations indexOps = elasticsearchOperations.indexOps(SearchListingDocument.class);

            log.info("Initializing Elasticsearch index for SearchListingDocument");

            try {

                if (!indexOps.exists()) {

                    log.info("Elasticsearch index does not exist. Creating index.");

                    boolean created = indexOps.create();

                    if (!created) {
                        throw new IllegalStateException("Failed to create Elasticsearch index");
                    }

                    log.info("Elasticsearch index created");
                }

                Document mapping = indexOps.createMapping(SearchListingDocument.class);
                indexOps.putMapping(mapping);

                log.info("Elasticsearch mapping applied");

            } catch (Exception ex) {

                log.error("Elasticsearch index initialization failed", ex);
                throw new IllegalStateException("Failed to initialize Elasticsearch index", ex);
            }
        };
    }
}