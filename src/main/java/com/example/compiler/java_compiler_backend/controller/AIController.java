package com.example.compiler.java_compiler_backend.controller;



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

    @PostMapping("/explain-code")
    public Map<String,String> explain(@RequestBody Map<String,String> body) throws Exception {

        String code = body.get("code");

        String prompt = "Explain this Java code step by step:\n" + code;

        String res = groq.askAI(prompt);

        return Map.of("explanation", res);
    }

    @PostMapping("/analyze")
    public Map<String,String> analyze(@RequestBody Map<String,String> body) throws Exception {

        String code = body.get("code");

        String prompt = "Analyze this Java code and give time complexity, space complexity and summary:\n" + code;

        return Map.of("analysis", groq.askAI(prompt));
    }

    @PostMapping("/hints")
    public Map<String,String> hints(@RequestBody Map<String,String> body) throws Exception {

        String code = body.get("code");

        String prompt = "Give hints to solve this Java problem without giving the solution:\n" + code;

        return Map.of("hint", groq.askAI(prompt));
    }

    @PostMapping("/solution")
    public Map<String,String> solution(@RequestBody Map<String,String> body) throws Exception {

        String code = body.get("code");

        String prompt = "Generate optimal Java solution with explanation:\n" + code;

        return Map.of("solution", groq.askAI(prompt));
    }

    @PostMapping("/patterns")
    public Map<String,String> patterns(@RequestBody Map<String,String> body) throws Exception {

        String code = body.get("code");

        String prompt = "Detect algorithm patterns used in this Java code:\n" + code;

        return Map.of("patterns", groq.askAI(prompt));
    }

    @PostMapping("/mistakes")
    public Map<String,String> mistakes(@RequestBody Map<String,String> body) throws Exception {

        String code = body.get("code");

        String prompt = "Find mistakes and bugs in this Java code:\n" + code;

        return Map.of("mistakes", groq.askAI(prompt));
    }

    @PostMapping("/chat")
    public Map<String,String> chat(@RequestBody Map<String,String> body) throws Exception {

        String msg = body.get("message");

        return Map.of("reply", groq.askAI(msg));
    }
}