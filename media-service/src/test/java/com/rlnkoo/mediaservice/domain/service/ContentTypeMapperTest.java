package com.rlnkoo.mediaservice.domain.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContentTypeMapperTest {

    private final ContentTypeMapper contentTypeMapper = new ContentTypeMapper();

    @Test
    void shouldReturnJpgExtensionForJpeg() {
        // when
        String result = contentTypeMapper.extensionFor("image/jpeg");

        // then
        assertEquals(".jpg", result);
    }

    @Test
    void shouldReturnPngExtensionForPng() {
        // when
        String result = contentTypeMapper.extensionFor("image/png");

        // then
        assertEquals(".png", result);
    }

    @Test
    void shouldReturnWebpExtensionForWebp() {
        // when
        String result = contentTypeMapper.extensionFor("image/webp");

        // then
        assertEquals(".webp", result);
    }

    @Test
    void shouldReturnJpgExtensionForJpegIgnoringCase() {
        // when
        String result = contentTypeMapper.extensionFor("IMAGE/JPEG");

        // then
        assertEquals(".jpg", result);
    }

    @Test
    void shouldReturnEmptyExtensionForUnknownContentType() {
        // when
        String result = contentTypeMapper.extensionFor("image/gif");

        // then
        assertEquals("", result);
    }

    @Test
    void shouldReturnEmptyExtensionForNullContentType() {
        // when
        String result = contentTypeMapper.extensionFor(null);

        // then
        assertEquals("", result);
    }
}