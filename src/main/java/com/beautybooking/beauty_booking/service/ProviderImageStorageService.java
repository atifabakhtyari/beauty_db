
package com.beautybooking.beauty_booking.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.UUID;

@Service
public class ProviderImageStorageService {

    private static final long MAX_SIZE = 5L * 1024 * 1024;

    private final Path directory;

    public ProviderImageStorageService(
            @Value("${app.upload.provider-dir:uploads/providers}")
            String directory
    ) {
        this.directory = Paths.get(directory)
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(this.directory);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "ساخت پوشه تصاویر سالن ناموفق بود.", e
            );
        }
    }

    public String save(MultipartFile file) {

        String extension = detectExtension(file);

        String filename = UUID.randomUUID() + "." + extension;

        Path target = directory.resolve(filename).normalize();

        if (!target.startsWith(directory)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "نام فایل نامعتبر است."
            );
        }

        try {
            Files.write(
                    target,
                    file.getBytes(),
                    StandardOpenOption.CREATE_NEW
            );
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "ذخیره تصویر ناموفق بود."
            );
        }

        return "/api/public/provider-images/" + filename;
    }

    public Path getPath(String filename) {

        if (filename == null ||
                !filename.matches(
                        "[a-fA-F0-9-]{36}\\.(jpg|png|webp)"
                )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "نام فایل نامعتبر است."
            );
        }

        Path path = directory.resolve(filename).normalize();

        if (!path.startsWith(directory) ||
                !Files.isRegularFile(path)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "تصویر پیدا نشد."
            );
        }

        return path;
    }

    public void delete(String imageUrl) {

        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        String prefix = "/api/public/provider-images/";

        if (!imageUrl.startsWith(prefix)) {
            return;
        }

        String filename = imageUrl.substring(prefix.length());

        try {
            Files.deleteIfExists(getPath(filename));
        } catch (Exception ignored) {
            // خطای حذف فایل نباید عملیات اصلی را خراب کند.
        }
    }

    private String detectExtension(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "لطفاً یک تصویر انتخاب کنید."
            );
        }

        if (file.getSize() > MAX_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "حجم تصویر نباید بیشتر از ۵ مگابایت باشد."
            );
        }

        try {
            byte[] bytes = file.getBytes();

            if (bytes.length >= 8 &&
                    (bytes[0] & 0xFF) == 0x89 &&
                    bytes[1] == 0x50 &&
                    bytes[2] == 0x4E &&
                    bytes[3] == 0x47 &&
                    bytes[4] == 0x0D &&
                    bytes[5] == 0x0A &&
                    bytes[6] == 0x1A &&
                    bytes[7] == 0x0A) {
                return "png";
            }

            if (bytes.length >= 3 &&
                    (bytes[0] & 0xFF) == 0xFF &&
                    (bytes[1] & 0xFF) == 0xD8 &&
                    (bytes[2] & 0xFF) == 0xFF) {
                return "jpg";
            }

            if (bytes.length >= 12 &&
                    new String(bytes, 0, 4, StandardCharsets.US_ASCII)
                            .equals("RIFF") &&
                    new String(bytes, 8, 4, StandardCharsets.US_ASCII)
                            .equals("WEBP")) {
                return "webp";
            }

        } catch (IOException ignored) {
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "فقط تصاویر JPG، PNG و WEBP مجاز هستند."
        );
    }
}
