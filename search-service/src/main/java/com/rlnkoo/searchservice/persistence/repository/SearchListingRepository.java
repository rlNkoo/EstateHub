package com.rlnkoo.searchservice.persistence.repository;

import com.rlnkoo.searchservice.domain.model.SearchListingDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SearchListingRepository extends ElasticsearchRepository<SearchListingDocument, UUID> {
}