package com.group01.controller;

import com.group01.service.DatabaseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DatabaseController {

    private final DatabaseService service;

    public DatabaseController(DatabaseService service) {
        this.service = service;
    }

    @GetMapping("/api/database")
    public String checkDatabase() {
        return service.checkConnection();
    }
}
