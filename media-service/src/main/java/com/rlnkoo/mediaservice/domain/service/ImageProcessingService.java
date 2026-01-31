package com.rlnkoo.mediaservice.domain.service;

import com.rlnkoo.mediaservice.domain.exception.StorageException;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Component
public class ImageProcessingService {

    private static final int THUMB_MAX_WIDTH = 320;

    public byte[] createJpegThumbnail(byte[] originalBytes) {
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(originalBytes));
            if (original == null) {
                throw new StorageException("Uploaded file is not a readable image");
            }

            int width = original.getWidth();
            int height = original.getHeight();

            if (width <= THUMB_MAX_WIDTH) {
                return toJpegBytes(original);
            }

            int newWidth = THUMB_MAX_WIDTH;
            int newHeight = (int) Math.round((double) height * newWidth / width);

            Image scaled = original.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
            BufferedImage thumb = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);

            Graphics2D g = thumb.createGraphics();
            try {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, newWidth, newHeight);
                g.drawImage(scaled, 0, 0, null);
            } finally {
                g.dispose();
            }

            return toJpegBytes(thumb);
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException("Cannot create thumbnail", e);
        }
    }

    private byte[] toJpegBytes(BufferedImage image) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean ok = ImageIO.write(image, "jpg", out);
        if (!ok) {
            throw new StorageException("JPG writer not available");
        }
        return out.toByteArray();
    }
}