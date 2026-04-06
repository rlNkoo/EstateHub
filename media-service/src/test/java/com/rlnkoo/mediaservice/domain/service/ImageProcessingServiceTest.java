package com.rlnkoo.mediaservice.domain.service;

import com.rlnkoo.mediaservice.domain.exception.StorageException;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ImageProcessingServiceTest {

    private final ImageProcessingService imageProcessingService = new ImageProcessingService();

    @Test
    void shouldCreateJpegThumbnailForLargeImage() throws Exception {
        // given
        byte[] originalBytes = createPngImageBytes(1000, 600);

        // when
        byte[] thumbnailBytes = imageProcessingService.createJpegThumbnail(originalBytes);

        // then
        assertNotNull(thumbnailBytes);
        assertTrue(thumbnailBytes.length > 0);

        BufferedImage thumbnail = ImageIO.read(new ByteArrayInputStream(thumbnailBytes));
        assertNotNull(thumbnail);
        assertEquals(320, thumbnail.getWidth());
        assertEquals(192, thumbnail.getHeight());
    }

    @Test
    void shouldReturnJpegBytesForSmallImageWithoutUpscaling() throws Exception {
        // given
        byte[] originalBytes = createPngImageBytes(200, 100);

        // when
        byte[] thumbnailBytes = imageProcessingService.createJpegThumbnail(originalBytes);

        // then
        assertNotNull(thumbnailBytes);
        assertTrue(thumbnailBytes.length > 0);

        BufferedImage thumbnail = ImageIO.read(new ByteArrayInputStream(thumbnailBytes));
        assertNotNull(thumbnail);
        assertEquals(200, thumbnail.getWidth());
        assertEquals(100, thumbnail.getHeight());
    }

    @Test
    void shouldThrowStorageExceptionWhenInputIsNotReadableImage() {
        // given
        byte[] invalidBytes = "not-an-image".getBytes();

        // when + then
        StorageException exception = assertThrows(
                StorageException.class,
                () -> imageProcessingService.createJpegThumbnail(invalidBytes)
        );

        assertEquals("Uploaded file is not a readable image", exception.getMessage());
    }

    @Test
    void shouldReturnJpegImageBytes() throws Exception {
        // given
        byte[] originalBytes = createPngImageBytes(400, 200);

        // when
        byte[] thumbnailBytes = imageProcessingService.createJpegThumbnail(originalBytes);

        // then
        BufferedImage thumbnail = ImageIO.read(new ByteArrayInputStream(thumbnailBytes));
        assertNotNull(thumbnail);

        boolean jpegReadable = ImageIO.getImageReadersByFormatName("jpg").hasNext();
        assertTrue(jpegReadable);
    }

    private byte[] createPngImageBytes(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        try {
            graphics.setColor(Color.BLUE);
            graphics.fillRect(0, 0, width, height);

            graphics.setColor(Color.WHITE);
            graphics.fillRect(width / 4, height / 4, width / 2, height / 2);
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        boolean written = ImageIO.write(image, "png", outputStream);
        assertTrue(written);

        return outputStream.toByteArray();
    }
}