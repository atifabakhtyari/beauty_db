
package com.beautybooking.beauty_booking.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// فقط در محیط توسعه فعال است و در محیط پیش‌فرض/تولید ثبت نمی‌شود.
@Profile("dev")
@RestController
@RequestMapping("/api")
public class AccessTestController {

    @GetMapping("/customer/test")
    public String customerTest() {
        return "Customer access granted";
    }

    @GetMapping("/provider/test/")
    public String providerTest() {
        return "Provider access granted";
    }

    @GetMapping("/admin/test")
    public String adminTest() {
        return "Admin access granted";
    }
}
