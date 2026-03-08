package com.example.compiler.java_compiler_backend.controller;



import com.example.compiler.java_compiler_backend.model.CodeRequest;
import com.example.compiler.java_compiler_backend.service.CodeExecutionService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class CodeController {

    private final CodeExecutionService executor;

    public CodeController(CodeExecutionService executor) {
        this.executor = executor;
    }

    @PostMapping("/run-java")
    public Map<String,Object> run(@RequestBody CodeRequest req) {

        return executor.runJava(req.getCode());
    }
}