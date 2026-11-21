package com.group01.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemController {

    @GetMapping("/api/health")
    public String health() {
        return "Vehicle Rental System is running!";
    }
}
