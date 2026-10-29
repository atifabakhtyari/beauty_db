
package com.beautybooking.beauty_booking.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
