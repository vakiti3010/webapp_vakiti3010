package com.csye6225.webapp.controller;

import com.csye6225.webapp.exception.InvalidContentLengthException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@RestController
public class HealthCheckController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/healthz")
    public ResponseEntity<Void> healthCheck(@RequestHeader(value = "Content-Length", defaultValue = "0") int contentLength, HttpServletRequest request) {
        if (contentLength > 0 || !request.getParameterMap().isEmpty()) {
            throw new InvalidContentLengthException("Invalid Content-Length or unexpected query parameters.");
        }

        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(1)) {
                throw new SQLException("Unable to establish a valid database connection");
            }
        } catch (Exception e) {
            throw new RuntimeException("Database connectivity issue");
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache().cachePrivate().mustRevalidate())
                .build();
    }
}

