package com.rlnkoo.listingservice.domain.exception;

public class InvalidCurrencyCodeException extends RuntimeException {

    public InvalidCurrencyCodeException(String currencyCode) {
        super("Invalid currencyCode: " + currencyCode);
    }
}
