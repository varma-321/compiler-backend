package com.example.compiler.java_compiler_backend.controller;

import com.example.compiler.java_compiler_backend.model.Admin;
import com.example.compiler.java_compiler_backend.service.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> request) {
        try {
            Admin admin = adminService.signup(
                    request.get("name"),
                    request.get("email"),
                    request.get("password")
            );
            return ResponseEntity.ok(Map.of("message", "Signup successful, pending approval"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            String token = adminService.login(
                    request.get("email"),
                    request.get("password")
            );
            return ResponseEntity.ok(Map.of("token", token));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/approve")
    public ResponseEntity<?> approve(@RequestParam String token) {
        boolean approved = adminService.approveAdmin(token);
        if (approved) {
            return ResponseEntity.ok("Admin approved successfully. The admin can now log in.");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired approval token.");
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(@RequestHeader("Authorization") String token) {
        // In a real app we'd validate the JWT via an interceptor or security filter.
        // For now, assume if the token is passed, frontend has logged in.
        // Dashboard returns mock/minimal data as Supabase is the actual users DB.
        if (token == null || !token.startsWith("Bearer ")) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(Map.of(
            "message", "Welcome to the Admin Dashboard",
            "stats", Map.of("usersActive", 42)
        ));
    }
}
