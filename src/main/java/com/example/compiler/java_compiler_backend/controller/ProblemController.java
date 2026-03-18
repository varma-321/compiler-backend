package com.example.compiler.java_compiler_backend.controller;

import com.example.compiler.java_compiler_backend.dto.ProblemResponseDTO;
import com.example.compiler.java_compiler_backend.dto.SubmissionRequestDTO;
import com.example.compiler.java_compiler_backend.dto.SubmissionResponseDTO;
import com.example.compiler.java_compiler_backend.service.ProblemService;
import com.example.compiler.java_compiler_backend.service.SubmissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService problemService;
    private final SubmissionService submissionService;

    public ProblemController(ProblemService problemService, SubmissionService submissionService) {
        this.problemService = problemService;
        this.submissionService = submissionService;
    }

    @GetMapping("/{key}")
    public ResponseEntity<?> getProblem(@PathVariable("key") String key, @RequestParam(value = "title", required = false) String title) {
        try {
            ProblemResponseDTO problem = problemService.getProblemByKey(key, title);
            return ResponseEntity.ok(problem);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/{key}/run")
    public ResponseEntity<?> runTests(@PathVariable("key") String key, @RequestBody SubmissionRequestDTO request) {
        try {
            SubmissionResponseDTO response = submissionService.submitCode(key, request.getCode(), false);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/{key}/submit")
    public ResponseEntity<?> submitCode(@PathVariable("key") String key, @RequestBody SubmissionRequestDTO request) {
        try {
            SubmissionResponseDTO response = submissionService.submitCode(key, request.getCode(), true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
