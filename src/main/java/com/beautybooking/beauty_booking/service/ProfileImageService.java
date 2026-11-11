
package com.beautybooking.beauty_booking.service;

import com.beautybooking.beauty_booking.entity.User;
import com.beautybooking.beauty_booking.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class ProfileImageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private final UserRepository userRepository;
    private final Path uploadDirectory;

    public ProfileImageService(
            UserRepository userRepository,
            @Value("${app.upload.profile-dir:uploads/profiles}")
            String uploadDirectory
    ) {
        this.userRepository = userRepository;
        this.uploadDirectory = Paths.get(uploadDirectory)
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(this.uploadDirectory);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "ساخت پوشه ذخیره عکس امکان‌پذیر نیست.", e
            );
        }
    }

    public User upload(String email, MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "لطفاً یک عکس انتخاب کنید."
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "حجم عکس نباید بیشتر از ۵ مگابایت باشد."
            );
        }

        String extension = detectImageExtension(file);

        User user = findUser(email);

        String filename = UUID.randomUUID() + "." + extension;

        Path target = uploadDirectory.resolve(filename).normalize();

        if (!target.startsWith(uploadDirectory)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "نام فایل نامعتبر است."
            );
        }

        try {
            Files.write(target, file.getBytes());
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "ذخیره عکس با خطا مواجه شد."
            );
        }

        String oldImageUrl = user.getProfileImageUrl();

        user.setProfileImageUrl(
                "/api/public/profile-images/" + filename
        );

        try {
            userRepository.save(user);
        } catch (RuntimeException e) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
            }
            throw e;
        }

        deleteOldImage(oldImageUrl);

        return user;
    }

    public User delete(String email) {

        User user = findUser(email);

        String oldImageUrl = user.getProfileImageUrl();

        if (oldImageUrl == null || oldImageUrl.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "این کاربر عکس پروفایل ندارد."
            );
        }

        user.setProfileImageUrl(null);
        userRepository.save(user);

        deleteOldImage(oldImageUrl);

        return user;
    }

    public Path getImagePath(String filename) {

        // فقط نام فایل‌های تولیدشده توسط سرور پذیرفته می‌شود.
        if (filename == null ||
                !filename.matches(
                        "[a-fA-F0-9-]{36}\\.(jpg|png|webp)"
                )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "نام فایل نامعتبر است."
            );
        }

        Path path = uploadDirectory.resolve(filename).normalize();

        if (!path.startsWith(uploadDirectory) ||
                !Files.isRegularFile(path)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "عکس پیدا نشد."
            );
        }

        return path;
    }

    private User findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "کاربر پیدا نشد."
                ));
    }

    private String detectImageExtension(MultipartFile file) {

        try {
            byte[] bytes = file.getBytes();

            // بررسی امضای واقعی فایل PNG
            if (bytes.length >= 8 &&
                    (bytes[0] & 0xFF) == 0x89 &&
                    bytes[1] == 0x50 &&
                    bytes[2] == 0x4E &&
                    bytes[3] == 0x47 &&
                    bytes[4] == 0x0D &&
                    bytes[5] == 0x0A &&
                    bytes[6] == 0x1A &&
                    bytes[7] == 0x0A) {

                BufferedImage image = ImageIO.read(
                        new ByteArrayInputStream(bytes)
                );

                if (image != null) {
                    return "png";
                }
            }

            // بررسی امضای واقعی فایل JPEG
            if (bytes.length >= 3 &&
                    (bytes[0] & 0xFF) == 0xFF &&
                    (bytes[1] & 0xFF) == 0xD8 &&
                    (bytes[2] & 0xFF) == 0xFF) {

                BufferedImage image = ImageIO.read(
                        new ByteArrayInputStream(bytes)
                );

                if (image != null) {
                    return "jpg";
                }
            }

            // بررسی امضای واقعی فایل WEBP
            if (bytes.length >= 12 &&
                    new String(bytes, 0, 4,
                            java.nio.charset.StandardCharsets.US_ASCII)
                            .equals("RIFF") &&
                    new String(bytes, 8, 4,
                            java.nio.charset.StandardCharsets.US_ASCII)
                            .equals("WEBP")) {

                return "webp";
            }

        } catch (IOException ignored) {
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "فرمت عکس معتبر نیست. فقط JPG، PNG و WEBP مجاز هستند."
        );
    }

    private void deleteOldImage(String imageUrl) {

        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        String prefix = "/api/public/profile-images/";

        if (!imageUrl.startsWith(prefix)) {
            return;
        }

        String filename = imageUrl.substring(prefix.length());

        try {
            Path oldPath = getImagePath(filename);
            Files.deleteIfExists(oldPath);
        } catch (Exception ignored) {
            // حذف فایل قدیمی نباید باعث شکست عملیات اصلی شود.
        }
    }
}
