
package com.beautybooking.beauty_booking.service;

import com.beautybooking.beauty_booking.dto.ProviderImageResponse;
import com.beautybooking.beauty_booking.dto.ProviderPublicResponse;
import com.beautybooking.beauty_booking.entity.Provider;
import com.beautybooking.beauty_booking.entity.ProviderImage;
import com.beautybooking.beauty_booking.entity.User;
import com.beautybooking.beauty_booking.repository.ProviderImageRepository;
import com.beautybooking.beauty_booking.repository.ProviderRepository;
import com.beautybooking.beauty_booking.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProviderPublicService {

    private final ProviderMediaService mediaService;
    private final ProviderImageRepository imageRepository;
    private final UserRepository userRepository;

    public ProviderPublicService(
            ProviderMediaService mediaService,
            ProviderImageRepository imageRepository,
            UserRepository userRepository
    ) {
        this.mediaService = mediaService;
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ProviderPublicResponse getPublicProfile(Long providerId) {

        Provider provider = mediaService.getApprovedProvider(providerId);

        User owner = userRepository.findById(provider.getUserId())
                .orElseThrow(() -> new IllegalStateException(
                        "حساب صاحب سالن پیدا نشد."
                ));

        List<ProviderImageResponse> images = imageRepository
                .findByProviderIdOrderByDisplayOrderAscCreatedAtDesc(
                        providerId
                )
                .stream()
                .map(this::toResponse)
                .toList();

        return new ProviderPublicResponse(
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
                provider.getCoverImageUrl(),
                owner.getProfileImageUrl(),
                images
        );
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
