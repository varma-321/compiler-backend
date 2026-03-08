package com.example.compiler.java_compiler_backend.controller;

import com.example.compiler.java_compiler_backend.model.CodeRequest;
import com.example.compiler.java_compiler_backend.service.CodeService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class CodeController {
    private final CodeService codeService;

    public CodeController(CodeService codeService) {
        this.codeService = codeService;
    }

    @PostMapping("/run-java")
    public Map<String, Object> runJava(@RequestBody CodeRequest request) {
        return codeService.runJavaCode(request.getCode());
    }

    @PostMapping("/explain-code")
    public Map<String, String> explainCode(@RequestBody CodeRequest request) {
        return codeService.explainCode(request.getCode());
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
