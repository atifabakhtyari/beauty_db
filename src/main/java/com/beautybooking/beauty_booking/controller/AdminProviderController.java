
package com.beautybooking.beauty_booking.controller;

import com.beautybooking.beauty_booking.dto.ProviderProfileResponse;
import com.beautybooking.beauty_booking.service.AdminProviderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/providers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProviderController {

    private final AdminProviderService adminProviderService;

    public AdminProviderController(
            AdminProviderService adminProviderService
    ) {
        this.adminProviderService = adminProviderService;
    }

    @GetMapping("/pending")
    public List<ProviderProfileResponse> getPendingProviders() {
        return adminProviderService.getPendingProviders();
    }

    @PutMapping("/{providerId}/approve")
    public ProviderProfileResponse approveProvider(
            @PathVariable Long providerId
    ) {
        return adminProviderService.approveProvider(providerId);
    }

    @PutMapping("/{providerId}/reject")
    public ProviderProfileResponse rejectProvider(
            @PathVariable Long providerId
    ) {
        return adminProviderService.rejectProvider(providerId);
    }
}
