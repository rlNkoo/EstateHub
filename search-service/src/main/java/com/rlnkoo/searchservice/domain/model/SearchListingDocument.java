package com.rlnkoo.searchservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "listings")
public class SearchListingDocument {

    @Id
    private UUID id;

    @Field(type = FieldType.Keyword)
    private UUID ownerId;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Integer)
    private Integer version;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant publishedAt;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Double)
    private BigDecimal priceAmount;

    @Field(type = FieldType.Keyword)
    private String currencyCode;

    @Field(type = FieldType.Keyword)
    private String country;

    @Field(type = FieldType.Keyword)
    private String city;

    @Field(type = FieldType.Keyword)
    private String street;

    @Field(type = FieldType.Keyword)
    private String postalCode;

    @Field(type = FieldType.Double)
    private BigDecimal area;

    @Field(type = FieldType.Integer)
    private Integer rooms;

    @Field(type = FieldType.Integer)
    private Integer floor;

    @Field(type = FieldType.Keyword)
    private String propertyType;

    @Field(type = FieldType.Keyword)
    private List<UUID> photoIds;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant indexedAt;
}