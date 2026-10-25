
package com.beautybooking.beauty_booking.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    @GetMapping
    public List<Map<String, Object>> getServices() {
        return List.of(
                Map.of(
                        "id", 1,
                        "name", "اصلاح مو",
                        "price", 250000,
                        "durationMinutes", 30
                ),
                Map.of(
                        "id", 2,
                        "name", "رنگ مو",
                        "price", 1200000,
                        "durationMinutes", 120
                ),
                Map.of(
                        "id", 3,
                        "name", "اصلاح ریش",
                        "price", 150000,
                        "durationMinutes", 20
                )
        );
    }
}
