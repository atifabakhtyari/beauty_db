
package com.beautybooking.beauty_booking.dto;

import com.beautybooking.beauty_booking.entity.ProviderType;

import java.util.List;

public record ProviderPublicResponse(
        Long providerId,
        Long userId,
        String providerName,
        ProviderType providerType,
        String city,
        String neighborhood,
        String address,
        String contactPhone,
        String introduction,
        String instagramUrl,
        String coverImageUrl,
        String ownerProfileImageUrl,
        List<ProviderImageResponse> images
) {
}
