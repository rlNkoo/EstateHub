package com.rlnkoo.searchservice.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.search")
public class SearchProperties {

    @NotBlank
    private String indexName;

    @Min(1)
    private int defaultPageSize = 20;

    @Min(1)
    private int maxPageSize = 100;
}