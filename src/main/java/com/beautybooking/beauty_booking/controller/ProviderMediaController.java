
package com.beautybooking.beauty_booking.controller;

import com.beautybooking.beauty_booking.dto.ProviderImageResponse;
import com.beautybooking.beauty_booking.dto.ProviderPublicResponse;
import com.beautybooking.beauty_booking.service.ProviderImageStorageService;
import com.beautybooking.beauty_booking.service.ProviderMediaService;
import com.beautybooking.beauty_booking.service.ProviderPublicService;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
public class ProviderMediaController {

    private final ProviderMediaService mediaService;
    private final ProviderPublicService publicService;
    private final ProviderImageStorageService storage;

    public ProviderMediaController(
            ProviderMediaService mediaService,
            ProviderPublicService publicService,
            ProviderImageStorageService storage
    ) {
        this.mediaService = mediaService;
        this.publicService = publicService;
        this.storage = storage;
    }

    // پروفایل عمومی سالن
    @GetMapping("/api/public/providers/{providerId}")
    public ProviderPublicResponse getPublicProfile(
            @PathVariable Long providerId
    ) {
        return publicService.getPublicProfile(providerId);
    }

    // گالری عمومی سالن
    @GetMapping("/api/public/providers/{providerId}/images")
    public List<ProviderImageResponse> getGallery(
            @PathVariable Long providerId
    ) {
        return mediaService.getPublicGallery(providerId);
    }

    // آپلود یا جایگزینی عکس اصلی سالن
    @PostMapping(
            value = "/api/provider/profile/cover-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('PROVIDER')")
    public Map<String, String> uploadCover(
            @RequestParam("file") MultipartFile file
    ) {
        return Map.of(
                "coverImageUrl",
                mediaService.uploadCover(file)
        );
    }

    // حذف عکس اصلی سالن
    @DeleteMapping("/api/provider/profile/cover-image")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<Void> deleteCover() {
        mediaService.deleteCover();
        return ResponseEntity.noContent().build();
    }

    // افزودن تصویر به گالری
    @PostMapping(
            value = "/api/provider/profile/images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('PROVIDER')")
    public ProviderImageResponse uploadGalleryImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("imageType") String imageType
    ) {
        return mediaService.uploadGalleryImage(file, imageType);
    }

    // حذف تصویر از گالری
    @DeleteMapping("/api/provider/profile/images/{imageId}")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<Void> deleteGalleryImage(
            @PathVariable Long imageId
    ) {
        mediaService.deleteGalleryImage(imageId);
        return ResponseEntity.noContent().build();
    }

    // دریافت فایل تصویر
    @GetMapping("/api/public/provider-images/{filename:.+}")
    public ResponseEntity<Resource> getImage(
            @PathVariable String filename
    ) throws IOException {

        Path path = storage.getPath(filename);
        Resource resource = new UrlResource(path.toUri());

        String extension = filename.substring(
                filename.lastIndexOf('.') + 1
        ).toLowerCase();

        MediaType mediaType = switch (extension) {
            case "png" -> MediaType.IMAGE_PNG;
            case "jpg" -> MediaType.IMAGE_JPEG;
            case "webp" -> MediaType.parseMediaType("image/webp");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noCache())
                .body(resource);
    }
}
