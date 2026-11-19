
package com.beautybooking.beauty_booking.controller;

import com.beautybooking.beauty_booking.dto.CreateServiceRequest;
import com.beautybooking.beauty_booking.dto.ServiceResponse;
import com.beautybooking.beauty_booking.dto.UpdateServiceRequest;
import com.beautybooking.beauty_booking.service.ServiceManagementService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/provider/services")
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderServiceController {

    private final ServiceManagementService serviceManagementService;

    public ProviderServiceController(
            ServiceManagementService serviceManagementService) {
        this.serviceManagementService = serviceManagementService;
    }

    // 1. دریافت فهرست خدمات و جست‌وجو بر اساس نام
    @GetMapping
    public ResponseEntity<List<ServiceResponse>> getServices(
            @RequestParam(required = false) String keyword) {

        List<ServiceResponse> services =
                serviceManagementService.getServices(keyword);

        return ResponseEntity.ok(services);
    }

    // 2. دریافت جزئیات یک خدمت
    @GetMapping("/{id}")
    public ResponseEntity<ServiceResponse> getService(
            @PathVariable Long id) {

        ServiceResponse service =
                serviceManagementService.getService(id);

        return ResponseEntity.ok(service);
    }

    // 3. ثبت خدمت جدید
    @PostMapping
    public ResponseEntity<ServiceResponse> createService(
            @Valid @RequestBody CreateServiceRequest request) {

        ServiceResponse service =
                serviceManagementService.createService(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(service);
    }

    // 4. ویرایش اطلاعات خدمت
    @PatchMapping("/{id}")
    public ResponseEntity<ServiceResponse> updateService(
            @PathVariable Long id,
            @Valid @RequestBody UpdateServiceRequest request) {

        ServiceResponse service =
                serviceManagementService.updateService(id, request);

        return ResponseEntity.ok(service);
    }

    // 5. حذف خدمت
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(
            @PathVariable Long id) {

        serviceManagementService.deleteService(id);

        return ResponseEntity.noContent().build();
    }
}
