package com.csye6225.webapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;

@RestController
public class HealthCheckController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/healthz")
    public ResponseEntity<Void> healthCheck(@RequestHeader(value = "Content-Length", defaultValue = "0") int contentLength) {
        if (contentLength > 0) {
            return ResponseEntity.badRequest().build();
        }

        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(1)) {
                return ResponseEntity
                        .ok()
                        .cacheControl(CacheControl.noCache().cachePrivate().mustRevalidate())
                        .build();
            }
            return ResponseEntity.status(503).header("Cache-Control", "no-cache").build();
        } catch (Exception e) {
            return ResponseEntity.status(503).header("Cache-Control", "no-cache").build();
        }
    }
}

