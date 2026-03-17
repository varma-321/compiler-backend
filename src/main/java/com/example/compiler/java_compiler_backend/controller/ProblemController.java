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
    public ResponseEntity<?> getProblem(@PathVariable String key, @RequestParam(required = false) String title) {
        try {
            ProblemResponseDTO problem = problemService.getProblemByKey(key, title);
            return ResponseEntity.ok(problem);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/{key}/run")
    public ResponseEntity<?> runTests(@PathVariable String key, @RequestBody SubmissionRequestDTO request) {
        try {
            // runAllTests = false means run only visible cases
            SubmissionResponseDTO response = submissionService.submitCode(key, request.getCode(), false);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/{key}/submit")
    public ResponseEntity<?> submitCode(@PathVariable String key, @RequestBody SubmissionRequestDTO request) {
        try {
            // runAllTests = true means run visible + hidden cases
            SubmissionResponseDTO response = submissionService.submitCode(key, request.getCode(), true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
