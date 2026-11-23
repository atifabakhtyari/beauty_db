
package com.beautybooking.beauty_booking.controller;

import com.beautybooking.beauty_booking.dto.CreateServiceCategoryRequest;
import com.beautybooking.beauty_booking.dto.ServiceCategoryListResponse;
import com.beautybooking.beauty_booking.dto.ServiceCategoryResponse;
import com.beautybooking.beauty_booking.service.ServiceManagementService;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/provider/service-categories")
public class ProviderServiceCategoryController {

    private final ServiceManagementService serviceManagementService;

    public ProviderServiceCategoryController(
            ServiceManagementService serviceManagementService) {

        this.serviceManagementService = serviceManagementService;
    }

    // دریافت فهرست دسته‌بندی‌های خدمات
    @GetMapping
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ServiceCategoryListResponse> getServiceCategories() {

        ServiceCategoryListResponse response =
                serviceManagementService.getServiceCategories();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    // ایجاد دسته‌بندی خدمات
    // دسته‌بندی‌ها داده مرجع سراسری و مشترک همه ارائه‌دهندگان هستند؛
    // بنابراین ایجاد آن‌ها فقط در اختیار مدیر است و هر ارائه‌دهنده‌ای
    // نمی‌تواند فهرست سراسری را تغییر دهد.
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "ایجاد دسته‌بندی خدمات",
            description = "یک دسته‌بندی سراسری جدید برای خدمات ایجاد می‌کند. "
                    + "این عملیات فقط برای نقش ADMIN مجاز است."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "دسته‌بندی با موفقیت ایجاد شد"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "ورودی نامعتبر است"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "احراز هویت نشده است"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "دسترسی مجاز نیست (فقط مدیر)"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "شناسه یا نام دسته‌بندی تکراری است"
            )
    })
    public ServiceCategoryResponse createServiceCategory(
            @Valid @RequestBody CreateServiceCategoryRequest request) {

        return serviceManagementService.createServiceCategory(request);
    }
}
