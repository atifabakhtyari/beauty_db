
package com.beautybooking.beauty_booking.service;

import com.beautybooking.beauty_booking.dto.ProviderProfileResponse;
import com.beautybooking.beauty_booking.entity.Provider;
import com.beautybooking.beauty_booking.entity.ProviderApprovalStatus;
import com.beautybooking.beauty_booking.exception.BusinessException;
import com.beautybooking.beauty_booking.exception.ResourceNotFoundException;
import com.beautybooking.beauty_booking.repository.ProviderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminProviderService {

    private final ProviderRepository providerRepository;

    public AdminProviderService(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    @Transactional(readOnly = true)
    public List<ProviderProfileResponse> getPendingProviders() {
        return providerRepository
                .findByApprovalStatus(ProviderApprovalStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProviderProfileResponse approveProvider(Long providerId) {
        Provider provider = findProvider(providerId);

        if (provider.getApprovalStatus()
                != ProviderApprovalStatus.PENDING) {
            throw new BusinessException(
                    "فقط پروفایل‌های در انتظار بررسی قابل تأیید هستند."
            );
        }

        provider.setApprovalStatus(ProviderApprovalStatus.APPROVED);
        provider.setActive(true);

        return toResponse(providerRepository.save(provider));
    }

    @Transactional
    public ProviderProfileResponse rejectProvider(Long providerId) {
        Provider provider = findProvider(providerId);

        if (provider.getApprovalStatus()
                != ProviderApprovalStatus.PENDING) {
            throw new BusinessException(
                    "فقط پروفایل‌های در انتظار بررسی قابل رد هستند."
            );
        }

        provider.setApprovalStatus(ProviderApprovalStatus.REJECTED);
        provider.setActive(false);

        return toResponse(providerRepository.save(provider));
    }

    private Provider findProvider(Long providerId) {
        return providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "پروفایل ارائه‌دهنده با شناسه "
                                + providerId + " پیدا نشد."
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
