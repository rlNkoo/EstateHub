package com.rlnkoo.mediaservice.api.media;

import com.rlnkoo.mediaservice.api.media.dto.PhotoResponse;
import com.rlnkoo.mediaservice.api.media.dto.UploadPhotoResponse;
import com.rlnkoo.mediaservice.domain.service.MediaService;
import com.rlnkoo.mediaservice.persistence.entity.MediaObjectEntity;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/media")
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/listings/{listingId}/photos")
    @ResponseStatus(HttpStatus.CREATED)
    public UploadPhotoResponse uploadPhoto(
            @PathVariable("listingId") UUID listingId,
            @RequestPart("file") @NotNull MultipartFile file
    ) {
        MediaObjectEntity entity = mediaService.uploadPhoto(listingId, file);

        URL url = mediaService.presignedUrlFor(entity);
        URL thumbUrl = mediaService.presignedThumbnailUrlFor(entity);

        return UploadPhotoResponse.builder()
                .mediaId(entity.getId())
                .listingId(entity.getListingId())
                .url(url)
                .thumbnailUrl(thumbUrl)
                .build();
    }

    @GetMapping("/listings/{listingId}/photos")
    public List<PhotoResponse> listListingPhotos(@PathVariable("listingId") UUID listingId) {
        List<MediaObjectEntity> photos = mediaService.listPhotos(listingId);

        List<PhotoResponse> result = new ArrayList<>();
        for (MediaObjectEntity entity : photos) {
            result.add(toPhotoResponse(entity));
        }
        return result;
    }

    @GetMapping("/photos/{mediaId}")
    public PhotoResponse getPhoto(@PathVariable("mediaId") UUID mediaId) {
        MediaObjectEntity entity = mediaService.getPhotoForRead(mediaId);
        return toPhotoResponse(entity);
    }

    @DeleteMapping("/photos/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePhoto(@PathVariable("mediaId") UUID mediaId) {
        mediaService.deletePhoto(mediaId);
    }

    private PhotoResponse toPhotoResponse(MediaObjectEntity entity) {
        URL url = mediaService.presignedUrlFor(entity);
        URL thumbUrl = mediaService.presignedThumbnailUrlFor(entity);

        return PhotoResponse.builder()
                .mediaId(entity.getId())
                .listingId(entity.getListingId())
                .contentType(entity.getContentType())
                .sizeBytes(entity.getSizeBytes())
                .sha256(entity.getSha256())
                .createdAt(entity.getCreatedAt())
                .url(url)
                .thumbnailUrl(thumbUrl)
                .build();
    }
}