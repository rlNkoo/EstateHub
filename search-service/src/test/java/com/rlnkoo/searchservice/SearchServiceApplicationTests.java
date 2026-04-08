package com.rlnkoo.searchservice;

import com.rlnkoo.searchservice.persistence.repository.SearchListingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class SearchServiceApplicationTests {

    @MockitoBean
    private SearchListingRepository searchListingRepository;

    @Test
    void contextLoads() {
    }
}