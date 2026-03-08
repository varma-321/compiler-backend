package com.example.compiler.java_compiler_backend.controller;



import com.example.compiler.java_compiler_backend.model.CodeRequest;
import com.example.compiler.java_compiler_backend.service.GroqService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class AIController {
    private final GroqService groq;

    public AIController(GroqService groq) {
        this.groq = groq;
    }

    @PostMapping("/analyze")
    public String analyze(@RequestBody CodeRequest req) throws Exception {
        return groq.analyzeCode(req.getCode());
    }

    @PostMapping("/hints")
    public String hints(@RequestBody CodeRequest req) throws Exception {
        String prompt = "Give a helpful hint to solve this Java code problem:\\n" + req.getCode();
        return groq.simplePrompt(prompt);
    }

    @PostMapping("/solution")
    public String solution(@RequestBody CodeRequest req) throws Exception {
        String prompt = "Generate the optimal Java solution for this code problem:\\n" + req.getCode();
        return groq.simplePrompt(prompt);
    }

    @PostMapping("/patterns")
    public String patterns(@RequestBody CodeRequest req) throws Exception {
        String prompt = "Detect algorithm patterns used in this Java code:\\n" + req.getCode();
        return groq.simplePrompt(prompt);
    }

    @PostMapping("/mistakes")
    public String mistakes(@RequestBody CodeRequest req) throws Exception {
        String prompt = "Find bugs or mistakes in this Java code:\\n" + req.getCode();
        return groq.simplePrompt(prompt);
    }

    @PostMapping("/chat")
    public String chat(@RequestBody CodeRequest req) throws Exception {
        return groq.simplePrompt(req.getCode());
    }
}