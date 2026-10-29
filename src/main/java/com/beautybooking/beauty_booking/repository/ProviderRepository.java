
package com.beautybooking.beauty_booking.repository;

import com.beautybooking.beauty_booking.entity.Provider;
import com.beautybooking.beauty_booking.entity.ProviderApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProviderRepository
        extends JpaRepository<Provider, Long> {

    Optional<Provider> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    List<Provider> findByApprovalStatus(
            ProviderApprovalStatus approvalStatus
    );
}
