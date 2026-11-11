
package com.beautybooking.beauty_booking.service;

import com.beautybooking.beauty_booking.dto.ProviderImageResponse;
import com.beautybooking.beauty_booking.entity.*;
import com.beautybooking.beauty_booking.exception.ResourceNotFoundException;
import com.beautybooking.beauty_booking.repository.*;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProviderMediaService {

    private final ProviderRepository providerRepository;
    private final ProviderImageRepository imageRepository;
    private final UserRepository userRepository;
    private final ProviderImageStorageService storage;

    public ProviderMediaService(
            ProviderRepository providerRepository,
            ProviderImageRepository imageRepository,
            UserRepository userRepository,
            ProviderImageStorageService storage
    ) {
        this.providerRepository = providerRepository;
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
        this.storage = storage;
    }

    @Transactional
    public String uploadCover(MultipartFile file) {

        Provider provider = getMyProvider();

        String newUrl = storage.save(file);
        String oldUrl = provider.getCoverImageUrl();

        provider.setCoverImageUrl(newUrl);
        providerRepository.save(provider);

        storage.delete(oldUrl);

        return newUrl;
    }

    @Transactional
    public void deleteCover() {

        Provider provider = getMyProvider();

        String oldUrl = provider.getCoverImageUrl();

        if (oldUrl == null || oldUrl.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "سالن عکس اصلی ندارد."
            );
        }

        provider.setCoverImageUrl(null);
        providerRepository.save(provider);

        storage.delete(oldUrl);
    }

    @Transactional
    public ProviderImageResponse uploadGalleryImage(
            MultipartFile file,
            String imageType
    ) {

        Provider provider = getMyProvider();

        if (imageType == null ||
                (!imageType.equals("SALON") &&
                        !imageType.equals("PORTFOLIO"))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "نوع عکس باید SALON یا PORTFOLIO باشد."
            );
        }

        long currentCount = imageRepository
                .findByProviderIdOrderByDisplayOrderAscCreatedAtDesc(
                        provider.getProviderId()
                ).size();

        if (currentCount >= 30) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "حداکثر ۳۰ تصویر در گالری مجاز است."
            );
        }

        String imageUrl = storage.save(file);

        ProviderImage image = new ProviderImage();
        image.setProviderId(provider.getProviderId());
        image.setImageUrl(imageUrl);
        image.setImageType(imageType);
        image.setDisplayOrder((int) currentCount);

        try {
            ProviderImage saved = imageRepository.save(image);
            return toResponse(saved);
        } catch (RuntimeException e) {
            storage.delete(imageUrl);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<ProviderImageResponse> getPublicGallery(
            Long providerId
    ) {
        Provider provider = getApprovedProvider(providerId);

        return imageRepository
                .findByProviderIdOrderByDisplayOrderAscCreatedAtDesc(
                        provider.getProviderId()
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteGalleryImage(Long imageId) {

        Provider provider = getMyProvider();

        ProviderImage image = imageRepository
                .findByImageIdAndProviderId(
                        imageId,
                        provider.getProviderId()
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "تصویر پیدا نشد یا متعلق به شما نیست."
                ));

        imageRepository.delete(image);
        storage.delete(image.getImageUrl());
    }

    @Transactional(readOnly = true)
    public Provider getApprovedProvider(Long providerId) {

        return providerRepository
                .findByProviderIdAndActiveTrueAndApprovalStatus(
                        providerId,
                        ProviderApprovalStatus.APPROVED
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "پروفایل عمومی سالن پیدا نشد."
                ));
    }

    private Provider getMyProvider() {

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication.getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "ابتدا وارد حساب کاربری شوید."
            );
        }

        User user = userRepository
                .findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "کاربر پیدا نشد."
                ));

        if (user.getRole() != UserRole.PROVIDER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "فقط ارائه‌دهنده خدمات می‌تواند تصاویر سالن را مدیریت کند."
            );
        }

        return providerRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ابتدا پروفایل ارائه‌دهنده را ایجاد کنید."
                ));
    }

    private ProviderImageResponse toResponse(ProviderImage image) {
        return new ProviderImageResponse(
                image.getImageId(),
                image.getImageUrl(),
                image.getImageType(),
                image.getDisplayOrder(),
                image.getCreatedAt()
        );
    }
}
