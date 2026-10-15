package com.eventflow.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<?> root(HttpServletRequest request) {
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        if (accept != null && accept.contains("text/html")) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("/swagger-ui/index.html"))
                    .build();
        }

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("service", "PulseEvent OS Backend API");
        info.put("status", "UP");
        info.put("version", "1.0.0");
        info.put("swaggerUi", "http://localhost:8085/swagger-ui/index.html");
        info.put("apiDocs", "http://localhost:8085/v3/api-docs");
        info.put("frontendApp", "http://localhost:5173");
        info.put("publicCatalog", "http://localhost:8085/api/events");
        info.put("healthCheck", "http://localhost:8085/api/health");
        info.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(info);
    }

    @GetMapping({"/api", "/api/health"})
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("service", "PulseEvent OS Backend API");
        health.put("status", "UP");
        health.put("swaggerUi", "http://localhost:8085/swagger-ui/index.html");
        health.put("apiDocs", "http://localhost:8085/v3/api-docs");
        health.put("frontendApp", "http://localhost:5173");
        health.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(health);
    }
}
