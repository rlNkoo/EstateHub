package com.rlnkoo.mediaservice.domain.service;

import com.rlnkoo.mediaservice.config.MediaProperties;
import com.rlnkoo.mediaservice.domain.exception.FileTooLargeException;
import com.rlnkoo.mediaservice.domain.exception.InvalidContentTypeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileValidationServiceTest {

    @Mock
    private MediaProperties mediaProperties;

    @InjectMocks
    private FileValidationService fileValidationService;

    @Test
    void shouldThrowIllegalArgumentExceptionWhenFileIsNull() {
        // when + then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fileValidationService.validate(null)
        );

        assertEquals("File is required", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenFileIsEmpty() {
        // given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        // when + then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fileValidationService.validate(emptyFile)
        );

        assertEquals("File is required", exception.getMessage());
    }

    @Test
    void shouldThrowFileTooLargeExceptionWhenFileExceedsLimit() {
        // given
        when(mediaProperties.getMaxFileSizeBytes()).thenReturn(5_242_880L);

        byte[] bytes = new byte[5_242_881];
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.jpg",
                "image/jpeg",
                bytes
        );

        // when + then
        FileTooLargeException exception = assertThrows(
                FileTooLargeException.class,
                () -> fileValidationService.validate(file)
        );

        assertEquals(
                "File too large: 5242881 bytes (max=5242880)",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowInvalidContentTypeExceptionWhenContentTypeIsNull() {
        // given
        when(mediaProperties.getMaxFileSizeBytes()).thenReturn(5_242_880L);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                null,
                "test".getBytes()
        );

        // when + then
        InvalidContentTypeException exception = assertThrows(
                InvalidContentTypeException.class,
                () -> fileValidationService.validate(file)
        );

        assertEquals("Invalid content type: null", exception.getMessage());
    }

    @Test
    void shouldThrowInvalidContentTypeExceptionWhenContentTypeIsBlank() {
        // given
        when(mediaProperties.getMaxFileSizeBytes()).thenReturn(5_242_880L);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                " ",
                "test".getBytes()
        );

        // when + then
        InvalidContentTypeException exception = assertThrows(
                InvalidContentTypeException.class,
                () -> fileValidationService.validate(file)
        );

        assertEquals("Invalid content type: null", exception.getMessage());
    }

    @Test
    void shouldThrowInvalidContentTypeExceptionWhenContentTypeIsNotAllowed() {
        // given
        when(mediaProperties.getMaxFileSizeBytes()).thenReturn(5_242_880L);
        when(mediaProperties.getAllowedContentTypes())
                .thenReturn(List.of("image/jpeg", "image/png", "image/webp"));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.gif",
                "image/gif",
                "test".getBytes()
        );

        // when + then
        InvalidContentTypeException exception = assertThrows(
                InvalidContentTypeException.class,
                () -> fileValidationService.validate(file)
        );

        assertEquals("Invalid content type: image/gif", exception.getMessage());
    }

    @Test
    void shouldAllowFileWhenContentTypeMatchesIgnoringCase() {
        // given
        when(mediaProperties.getMaxFileSizeBytes()).thenReturn(5_242_880L);
        when(mediaProperties.getAllowedContentTypes())
                .thenReturn(List.of("image/jpeg", "image/png", "image/webp"));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "IMAGE/JPEG",
                "valid-image".getBytes()
        );

        // when + then
        assertDoesNotThrow(() -> fileValidationService.validate(file));
    }

    @Test
    void shouldPassValidationForAllowedContentTypeAndValidSize() {
        // given
        when(mediaProperties.getMaxFileSizeBytes()).thenReturn(5_242_880L);
        when(mediaProperties.getAllowedContentTypes())
                .thenReturn(List.of("image/jpeg", "image/png", "image/webp"));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.webp",
                "image/webp",
                "valid-image".getBytes()
        );

        // when + then
        assertDoesNotThrow(() -> fileValidationService.validate(file));
    }
}