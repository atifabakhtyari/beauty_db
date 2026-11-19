
package com.beautybooking.beauty_booking.repository;

import com.beautybooking.beauty_booking.entity.ProviderService;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProviderServiceRepository
        extends JpaRepository<ProviderService, Long> {

    // دریافت تمام خدمات یک ارائه‌دهنده
    // دسته‌بندی به صورت fetch join بارگذاری می‌شود تا N+1 رخ ندهد.
    @EntityGraph(attributePaths = "category")
    List<ProviderService> findAllByProviderId(Long providerId);

    // دریافت یک خدمت مشخص متعلق به یک ارائه‌دهنده
    @EntityGraph(attributePaths = "category")
    Optional<ProviderService> findByServiceIdAndProviderId(
            Long serviceId,
            Long providerId
    );

    // بررسی وجود خدمت با نام مشخص برای یک ارائه‌دهنده
    boolean existsByProviderIdAndServiceNameIgnoreCase(
            Long providerId,
            String serviceName
    );

    // بررسی تکراری بودن نام در ویرایش (به جز خود خدمت)
    boolean existsByProviderIdAndServiceNameIgnoreCaseAndServiceIdNot(
            Long providerId,
            String serviceName,
            Long serviceId
    );

    // جست‌وجوی خدمات بر اساس بخشی از نام خدمت
    @EntityGraph(attributePaths = "category")
    List<ProviderService> findByProviderIdAndServiceNameContainingIgnoreCase(
            Long providerId,
            String serviceName
    );

    // غیرفعال‌سازی تمام خدمات یک ارائه‌دهنده (مثلاً هنگام رد شدن پروفایل)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ProviderService s
            SET s.isActive = false
            WHERE s.providerId = :providerId
              AND s.isActive = true
            """)
    int deactivateAllByProviderId(@Param("providerId") Long providerId);
}
