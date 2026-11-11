
package com.beautybooking.beauty_booking.controller;

import com.beautybooking.beauty_booking.dto.RegisterResponse;
import com.beautybooking.beauty_booking.entity.User;
import com.beautybooking.beauty_booking.service.ProfileImageService;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class ProfileImageController {

    private final ProfileImageService profileImageService;

    public ProfileImageController(
            ProfileImageService profileImageService
    ) {
        this.profileImageService = profileImageService;
    }

    @PostMapping(
            value = "/me/profile-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public RegisterResponse uploadProfileImage(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) {

        User user = profileImageService.upload(
                authentication.getName(),
                file
        );

        return toResponse(user);
    }

    @DeleteMapping("/me/profile-image")
    public RegisterResponse deleteProfileImage(
            Authentication authentication
    ) {

        User user = profileImageService.delete(
                authentication.getName()
        );

        return toResponse(user);
    }

    // نمایش عکس بدون نیاز به ورود
    @GetMapping("/public/profile-images/{filename:.+}")
    public ResponseEntity<Resource> getProfileImage(
            @PathVariable String filename
    ) {

        try {
            Path imagePath = profileImageService.getImagePath(filename);
            Resource resource = new UrlResource(imagePath.toUri());

            String extension = filename.substring(
                    filename.lastIndexOf('.') + 1
            ).toLowerCase();

            MediaType mediaType = switch (extension) {
                case "png" -> MediaType.IMAGE_PNG;
                case "webp" -> MediaType.parseMediaType("image/webp");
                case "jpg" -> MediaType.IMAGE_JPEG;
                default -> throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "فرمت عکس معتبر نیست."
                );
            };

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .cacheControl(
                            CacheControl.maxAge(1, TimeUnit.HOURS)
                    ).header(
                            "X-Content-Type-Options",
                            "nosniff"
                    )
                    .body(resource);

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "عکس پیدا نشد."
            );
        }
    }

    private RegisterResponse toResponse(User user) {
        return new RegisterResponse(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getAge(),
                user.getProfileImageUrl()
        );
    }
}
