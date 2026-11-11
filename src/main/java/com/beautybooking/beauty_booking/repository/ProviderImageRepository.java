
package com.beautybooking.beauty_booking.repository;

import com.beautybooking.beauty_booking.entity.ProviderImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProviderImageRepository
        extends JpaRepository<ProviderImage, Long> {

    List<ProviderImage> findByProviderIdOrderByDisplayOrderAscCreatedAtDesc(
            Long providerId
    );

    Optional<ProviderImage> findByImageIdAndProviderId(
            Long imageId,
            Long providerId
    );
}
