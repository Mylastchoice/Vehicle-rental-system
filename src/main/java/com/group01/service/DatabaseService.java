package com.group01.service;

import com.group01.repository.DatabaseRepository;
import org.springframework.stereotype.Service;

@Service
public class DatabaseService {

    private final DatabaseRepository repository;

    public DatabaseService(DatabaseRepository repository) {
        this.repository = repository;
    }

    public String checkConnection() {
        return "Connected to: "
                + repository.getDatabaseName();
    }
}
