
package com.beautybooking.beauty_booking.repository;

import com.beautybooking.beauty_booking.entity.ProviderService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProviderServiceRepository
        extends JpaRepository<ProviderService, Long> {

    // دریافت تمام خدمات یک ارائه‌دهنده
    List<ProviderService> findAllByProviderId(Long providerId);

    // دریافت یک خدمت مشخص متعلق به یک ارائه‌دهنده
    Optional<ProviderService> findByServiceIdAndProviderId(
            Long serviceId,
            Long providerId
    );

    // بررسی وجود خدمت با نام مشخص برای یک ارائه‌دهنده
    boolean existsByProviderIdAndServiceNameIgnoreCase(
            Long providerId,
            String serviceName
    );

    // جست‌وجوی خدمات بر اساس بخشی از نام خدمت
    List<ProviderService> findByProviderIdAndServiceNameContainingIgnoreCase(
            Long providerId,
            String serviceName
    );
}
