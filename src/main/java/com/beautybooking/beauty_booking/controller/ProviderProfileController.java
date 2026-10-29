
package com.beautybooking.beauty_booking.controller;

import com.beautybooking.beauty_booking.dto.ProviderProfileRequest;
import com.beautybooking.beauty_booking.dto.ProviderProfileResponse;
import com.beautybooking.beauty_booking.service.ProviderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/provider/profile")
public class ProviderProfileController {

    private final ProviderService providerService;

    public ProviderProfileController(ProviderService providerService) {
        this.providerService = providerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PROVIDER')")
    public ProviderProfileResponse createProfile(
            @Valid @RequestBody ProviderProfileRequest request
    ) {
        return providerService.createProfile(request);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PROVIDER')")
    public ProviderProfileResponse getMyProfile() {
        return providerService.getMyProfile();
    }
}
