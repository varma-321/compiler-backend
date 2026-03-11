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

    @PostMapping("/explain-code")
    public Map<String, String> explainCode(@RequestBody CodeRequest req) throws Exception {

        String prompt = "Explain this Java code:\n" + req.getCode();

        String response = groq.askAI(prompt);

        return Map.of("explanation", response);
    }

    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody ChatRequest req) throws Exception {

        String response = groq.askAI(req.getMessage());

        return Map.of("reply", response);
    }

    @PostMapping("/generate-test-cases")
    public Map<String, Object> generateTestCases(@RequestBody CodeRequest req) throws Exception {
        String prompt = "You are a test case generator for Java DSA problems. Generate exactly 3 high-quality test cases:\n" +
                "1. NORMAL CASE: A standard test case matching the problem description with typical inputs\n" +
                "2. EDGE CASE: Edge cases like empty arrays, single elements, null/zero inputs\n" +
                "3. BOUNDARY CASE: Max/min constraints, large values, off-by-one scenarios\n" +
                "\n" +
                "CRITICAL: Each test case must have MULTIPLE INPUT VARIABLES matching the function parameters.\n" +
                "\n" +
                "Return ONLY a valid JSON array of objects. Do not wrap in markdown or backticks. Format:\n" +
                "[\n" +
                "  {\n" +
                "    \"inputs\": { \"nums\": \"[2,7,11,15]\", \"target\": \"9\" },\n" +
                "    \"expectedOutput\": \"[0, 1]\",\n" +
                "    \"category\": \"normal\"\n" +
                "  }\n" +
                "]\n" +
                "\n" +
                "All input values must be STRING representations. Expected output must exactly match what System.out.println() would produce in Java.\n" +
                "\n" +
                "Generate for this Java function:\n" + req.getCode();

        String response = groq.askAI(prompt);
        // Remove markdown formatting if the AI includes it
        if (response.startsWith("```json")) {
            response = response.substring(7);
        }
        if (response.startsWith("```")) {
            response = response.substring(3);
        }
        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }
        response = response.trim();

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> testCases = mapper.readValue(response, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
        return Map.of("testCases", testCases);
    }
}