package com.example.compiler.java_compiler_backend.controller;



import com.example.compiler.java_compiler_backend.model.ChatRequest;
import com.example.compiler.java_compiler_backend.model.CodeRequest;
import com.example.compiler.java_compiler_backend.service.GroqService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    public Map<String, Object> analyze(@RequestBody CodeRequest req) throws Exception {

        String result = groq.analyzeCode(req.getCode());

        ObjectMapper mapper = new ObjectMapper();

        return mapper.readValue(result, Map.class);
    }

    @PostMapping("/hints")
    public Map<String, String> hints(@RequestBody CodeRequest req) throws Exception {

        String prompt = "Give a hint for solving this Java code problem:\n" + req.getCode();

        String response = groq.askAI(prompt);

        return Map.of("hint", response);
    }

    @PostMapping("/solution")
    public Map<String, String> solution(@RequestBody CodeRequest req) throws Exception {

        String prompt = "Generate an optimal Java solution for:\n" + req.getCode();

        String response = groq.askAI(prompt);

        return Map.of("explanation", response);
    }

    @PostMapping("/patterns")
    public Map<String, String> patterns(@RequestBody CodeRequest req) throws Exception {

        String prompt = "Detect algorithm patterns in this Java code:\n" + req.getCode();

        String response = groq.askAI(prompt);

        return Map.of("patterns", response);
    }

    @PostMapping("/mistakes")
    public Map<String, String> mistakes(@RequestBody CodeRequest req) throws Exception {

        String prompt = "Find mistakes in this Java code:\n" + req.getCode();

        String response = groq.askAI(prompt);

        return Map.of("mistakes", response);
    }

    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody ChatRequest req) throws Exception {

        String response = groq.askAI(req.getMessage());

        return Map.of("reply", response);
    }
}