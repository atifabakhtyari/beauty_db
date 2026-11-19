
package com.beautybooking.beauty_booking.controller;

import com.beautybooking.beauty_booking.dto.ServiceCategoryListResponse;
import com.beautybooking.beauty_booking.service.ServiceManagementService;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/provider/service-categories")
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderServiceCategoryController {

    private final ServiceManagementService serviceManagementService;

    public ProviderServiceCategoryController(
            ServiceManagementService serviceManagementService) {

        this.serviceManagementService = serviceManagementService;
    }

    // دریافت فهرست دسته‌بندی‌های خدمات
    @GetMapping
    public ResponseEntity<ServiceCategoryListResponse> getServiceCategories() {

        ServiceCategoryListResponse response =
                serviceManagementService.getServiceCategories();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }
}
