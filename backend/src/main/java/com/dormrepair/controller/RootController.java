package com.dormrepair.controller;

import com.dormrepair.support.ApiResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class RootController {
    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String index() {
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8">
                  <title>Dorm Repair Backend</title>
                  <style>
                    body { font-family: Arial, sans-serif; margin: 48px; line-height: 1.7; color: #1f2937; }
                    a { color: #2563eb; }
                    code { background: #f3f4f6; padding: 2px 6px; border-radius: 4px; }
                  </style>
                </head>
                <body>
                  <h1>Dorm Repair Backend is running</h1>
                  <p>Java Spring Boot REST API service is available.</p>
                  <p>Frontend: <a href="http://localhost:5173">http://localhost:5173</a></p>
                  <p>Health API: <a href="/api/health"><code>/api/health</code></a></p>
                  <p>Public notices API: <a href="/api/public/notices"><code>/api/public/notices</code></a></p>
                </body>
                </html>
                """;
    }

    @GetMapping("/api/health")
    public ApiResult<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("service", "dorm-repair-backend");
        data.put("time", LocalDateTime.now());
        return ApiResult.ok(data);
    }
}
