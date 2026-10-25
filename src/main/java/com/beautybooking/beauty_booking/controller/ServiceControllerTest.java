//
//package com.beautybooking.beauty_booking.controller;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.test.web.servlet.MockMvc;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@WebMvcTest(ServiceController.class)
//class ServiceControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Test
//    void shouldReturnThreeServices() throws Exception {
//        mockMvc.perform(get("/api/services"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(3))
//                .andExpect(jsonPath("$[0].name").value("اصلاح مو"));
//    }
//}
