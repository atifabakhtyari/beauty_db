package com.beautybooking.beauty_booking.repository;

import com.beautybooking.beauty_booking.entity.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServiceCategoryRepository
        extends JpaRepository<ServiceCategory, String> {

    List<ServiceCategory> findTop200ByOrderByNameAsc();

    // نام دسته‌بندی بدون توجه به حروف بزرگ/کوچک و فاصله‌های ابتدا و انتها
    // مقایسه می‌شود تا از ثبت نام تکراری جلوگیری شود.
    @Query("""
            select count(c)
            from ServiceCategory c
            where lower(trim(c.name)) = :normalizedName
            """)
    long countByNormalizedName(@Param("normalizedName") String normalizedName);
}
