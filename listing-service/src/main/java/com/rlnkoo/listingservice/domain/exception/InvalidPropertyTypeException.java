package com.rlnkoo.listingservice.domain.exception;

public class InvalidPropertyTypeException extends RuntimeException {

    public InvalidPropertyTypeException(String propertyType) {
        super("Invalid propertyType: " + propertyType);
    }
}