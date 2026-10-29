
package com.beautybooking.beauty_booking.service;

import com.beautybooking.beauty_booking.dto.ProviderProfileRequest;
import com.beautybooking.beauty_booking.dto.ProviderProfileResponse;
import com.beautybooking.beauty_booking.entity.Provider;
import com.beautybooking.beauty_booking.entity.ProviderApprovalStatus;
import com.beautybooking.beauty_booking.entity.User;
import com.beautybooking.beauty_booking.entity.UserRole;
import com.beautybooking.beauty_booking.exception.BusinessException;
import com.beautybooking.beauty_booking.exception.ResourceNotFoundException;
import com.beautybooking.beauty_booking.repository.ProviderRepository;
import com.beautybooking.beauty_booking.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderService {




    private final ProviderRepository providerRepository;
    private final UserRepository userRepository;

    public ProviderService(
            ProviderRepository providerRepository,
            UserRepository userRepository
    ) {
        this.providerRepository = providerRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProviderProfileResponse createProfile(
            ProviderProfileRequest request
    ) {
        User user = getAuthenticatedUser();

        if (user.getRole() != UserRole.PROVIDER) {
            throw new BusinessException(
                    "فقط کاربران دارای نقش PROVIDER می‌توانند پروفایل ارائه‌دهنده بسازند."
            );
        }

        if (providerRepository.existsByUserId(user.getUserId())) {
            throw new BusinessException(
                    "برای این حساب قبلاً پروفایل ارائه‌دهنده ساخته شده است."
            );
        }

        Provider provider = new Provider();

        // شناسه از کاربر احراز هویت‌شده گرفته می‌شود.
        // هیچ userId از درخواست کلاینت پذیرفته نمی‌شود.
        provider.setUserId(user.getUserId());

        provider.setProviderName(request.providerName().trim());
        provider.setProviderType(request.providerType());
        provider.setCity(request.city().trim());
        provider.setNeighborhood(request.neighborhood().trim());
        provider.setAddress(request.address().trim());
        provider.setContactPhone(request.contactPhone().trim());
        provider.setIntroduction(request.introduction());
        provider.setInstagramUrl(request.instagramUrl());

        provider.setApprovalStatus(ProviderApprovalStatus.PENDING);
        provider.setActive(true);

        Provider saved = providerRepository.save(provider);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProviderProfileResponse getMyProfile() {
        User user = getAuthenticatedUser();

        if (user.getRole() != UserRole.PROVIDER) {
            throw new BusinessException(
                    "این بخش فقط برای ارائه‌دهندگان است."
            );
        }

        Provider provider = providerRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "پروفایل ارائه‌دهنده هنوز تکمیل نشده است."
                ));

        return toResponse(provider);
    }

    @Transactional(readOnly = true)
    public void requireApprovedProvider() {
        User user = getAuthenticatedUser();

        if (user.getRole() != UserRole.PROVIDER) {
            throw new BusinessException(
                    "این عملیات فقط برای ارائه‌دهندگان مجاز است."
            );
        }

        Provider provider = providerRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new BusinessException(
                        "ابتدا پروفایل ارائه‌دهنده را تکمیل کنید."
                ));

        if (!provider.isActive()
                || provider.getApprovalStatus()
                != ProviderApprovalStatus.APPROVED) {
            throw new BusinessException(
                    "پروفایل شما هنوز توسط مدیر تأیید نشده یا غیرفعال است."
            );
        }
    }

    private User getAuthenticatedUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {
            throw new BusinessException(
                    "برای انجام این عملیات ابتدا وارد حساب شوید."
            );
        }

        String email = authentication.getName();

        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "کاربر احراز هویت‌شده در دیتابیس پیدا نشد."
                ));
    }

    private ProviderProfileResponse toResponse(Provider provider) {
        return new ProviderProfileResponse(
                provider.getProviderId(),
                provider.getUserId(),
                provider.getProviderName(),
                provider.getProviderType(),
                provider.getCity(),
                provider.getNeighborhood(),
                provider.getAddress(),
                provider.getContactPhone(),
                provider.getIntroduction(),
                provider.getInstagramUrl(),
                provider.isActive(),
                provider.getApprovalStatus()
        );
    }
}
