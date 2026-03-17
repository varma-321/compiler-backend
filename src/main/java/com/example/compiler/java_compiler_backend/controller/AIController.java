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
        String system = "You are an expert Java Code Analyst. Analyze the code for logic, efficiency, and JDK 17 best practices.";
        String user = "Problem: " + (req.getProblemId() != null ? req.getProblemId() : "Generic Java Task") + "\nCode:\n" + req.getCode();
        String result = groq.askAI(system, user);
        return Map.of("analysis", result);
    }

    @PostMapping("/analyze-advanced")
    public Map<String, String> analyzeAdvanced(@RequestBody CodeRequest req) throws Exception {
        String result = groq.analyzeComplexityAdvanced(req.getCode());
        return Map.of("radar", result);
    }

    @PostMapping("/dry-run")
    public Map<String, String> dryRun(@RequestBody CodeRequest req) throws Exception {
        String result = groq.dryRun(req.getCode(), req.getInputs());
        return Map.of("trace", result);
    }

    @PostMapping("/hints")
    public Map<String, String> hints(@RequestBody CodeRequest req) throws Exception {
        String system = "You are a professional DSA Mentor. Give a subtle hint without spoiling the solution.";
        String prompt = "Language: Java\nProblem: " + req.getProblemId() + "\nCode:\n" + req.getCode();
        String response = groq.askAI(system, prompt);
        return Map.of("hint", response);
    }

    @PostMapping("/solution")
    public Map<String, String> solution(@RequestBody CodeRequest req) throws Exception {
        String type = req.getType() != null ? req.getType() : "optimal";
        String system = "You are an Elite Java Architect. Provide a " + type + " solution using modern Java idioms.";
        String prompt = "Generate a " + type + " Java solution for problem: " + req.getProblemId() + "\nCurrent Code:\n" + req.getCode();
        String response = groq.askAI(system, prompt);
        return Map.of("solution", response);
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
                "Generate for this Java function:\n" + req.getCode();

        String response = groq.askAI(prompt);
        if (response.startsWith("```json")) response = response.substring(7);
        if (response.startsWith("```")) response = response.substring(3);
        if (response.endsWith("```")) response = response.substring(0, response.length() - 3);
        response = response.trim();

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> testCases = mapper.readValue(response, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
        return Map.of("testCases", testCases);
    }

    @PostMapping("/mistakes")
    public Map<String, String> mistakes(@RequestBody CodeRequest req) throws Exception {
        String system = "You are a Java Code Bug Hunter. Find logic errors, runtime exceptions, edge case failures, and Java anti-patterns in the code. Be thorough and actionable.";
        String prompt = "Find all mistakes and bugs in this Java code for problem: " + req.getProblemId() + "\n\n" + req.getCode();
        String response = groq.askAI(system, prompt);
        return Map.of("mistakes", response);
    }

    @PostMapping("/patterns")
    public Map<String, String> patterns(@RequestBody CodeRequest req) throws Exception {
        String system = "You are a DSA Pattern Expert. Identify which classic algorithmic patterns are being used and suggest better alternatives if applicable.";
        String prompt = "Identify algorithm patterns in this Java code:\n\n" + req.getCode();
        String response = groq.askAI(system, prompt);
        return Map.of("patterns", response);
    }

    @PostMapping("/explain-code")
    public Map<String, String> explainCode(@RequestBody CodeRequest req) throws Exception {
        String system = "You are an expert Java tutor. Break down the code line-by-line and explain every decision in simple, clear terms for a student.";
        String prompt = "Explain this Java code for: " + req.getProblemId() + "\n\n" + req.getCode();
        String response = groq.askAI(system, prompt);
        return Map.of("explanation", response);
    }

    @PostMapping("/time-complexity")
    public Map<String, String> timeComplexity(@RequestBody CodeRequest req) throws Exception {
        String system = "You are a Java Performance Analyst. Calculate and explain the time complexity of the given code with loop analysis.";
        String prompt = "Calculate the exact time complexity of this Java code with step-by-step loop analysis:\n\n" + req.getCode();
        String response = groq.askAI(system, prompt);
        return Map.of("result", response);
    }

    @PostMapping("/space-complexity")
    public Map<String, String> spaceComplexity(@RequestBody CodeRequest req) throws Exception {
        String system = "You are a Java Memory Analyst. Calculate and explain the space complexity of the given code.";
        String prompt = "Calculate the space complexity (memory usage) of this Java code:\n\n" + req.getCode();
        String response = groq.askAI(system, prompt);
        return Map.of("result", response);
    }

    @PostMapping("/java-interview")
    public Map<String, String> javaInterview(@RequestBody CodeRequest req) throws Exception {
        String system = "You are a FAANG Java Interviewer. Based on the code and problem, generate 5 likely follow-up interview questions and their answers.";
        String prompt = "The user is solving: " + req.getProblemId() + " in Java. Generate 5 follow-up interview questions with answers based on this code:\n\n" + req.getCode();
        String response = groq.askAI(system, prompt);
        return Map.of("questions", response);
    }

    @PostMapping("/approach")
    public Map<String, String> approach(@RequestBody CodeRequest req) throws Exception {
        String system = "You are an elite DSA strategist. Suggest the optimal algorithmic approach and data structure choices for the given problem.";
        String prompt = "Problem: " + req.getProblemId() + "\nSuggest the best approach and data structures to solve this efficiently in Java:\n\n" + req.getCode();
        String response = groq.askAI(system, prompt);
        return Map.of("approach", response);
    }

    @PostMapping("/refactor")
    public Map<String, String> refactor(@RequestBody CodeRequest req) throws Exception {
        String system = "You are a Java Code Refactoring Expert. Rewrite the code following clean code principles, SOLID, and modern Java 17+ features.";
        String prompt = "Refactor this Java code for readability, maintainability, and modern Java 17 best practices:\n\n" + req.getCode();
        String response = groq.askAI(system, prompt);
        return Map.of("refactored", response);
    }

    @PostMapping("/vibe-check")
    public Map<String, Object> vibeCheck(@RequestBody CodeRequest req) throws Exception {
        String system = """
            You are a Java Code 'Vibe' Judge. Evaluate the user's code across 4 dimensions (0-100):
            1. Readability (Names, Formatting, Structure)
            2. Performance (Efficiency, Collections choice)
            3. Scalability (SOLID, Design patterns)
            4. Java Idioms (Modern JDK 17+ usage)
            
            Also assign a 'Profile' rank:
            - Gosling's Disciple (90+)
            - Clean Coder (75-89)
            - Junior Explorer (50-74)
            - Spaghetti Overlord (<50)
            
            Return ONLY valid JSON (no markdown):
            {
              "scores": {"readability": 85, "performance": 70, "scalability": 60, "javaIdioms": 90},
              "profile": "Clean Coder",
              "summary": "Short punchy summary of the 'vibe'."
            }
            """;
        String prompt = "Rate the 'vibe' of this Java code:\n\n" + req.getCode();
        String response = groq.askAI(system, prompt, "llama-3.3-70b-versatile");
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start != -1 && end != -1) response = response.substring(start, end + 1);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(response, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
    }

    @PostMapping("/performance-audit")
    public Map<String, String> performanceAudit(@RequestBody CodeRequest req) throws Exception {
        String system = "You are a JVM Performance Engineer. Audit the code for low-level optimizations (Memory, GC overhead, JIT friendliness, Concurrency). Provide an 'Epic' deep dive.";
        String prompt = "Perform a Deep Performance Audit on this Java code:\n\n" + req.getCode();
        String response = groq.askAI(system, prompt, "llama-3.3-70b-versatile");
        return Map.of("audit", response);
    }

    @PostMapping("/visualize")
    public Map<String, String> visualize(@RequestBody CodeRequest req) throws Exception {
        String system = """
            You are a Logic Visualization Master. Translate the Java code into a Mermaid.js flowchart (graph TD).
            RULES:
            1. Render ONLY the logic flow, branches, and loops.
            2. CRITICAL: All node labels MUST be double-quoted. Example: A["Read nums[i]"]
            3. Avoid using brackets like [] or () inside labels unless escaped.
            4. Return ONLY the Mermaid string, starting with 'graph TD'. No markdown wrappers.
            """;
        String prompt = "Visualize this Java code as a Mermaid flowchart:\n\n" + req.getCode();
        String response = groq.askAI(system, prompt, "llama-3.3-70b-versatile");
        response = response.replaceAll("```mermaid", "").replaceAll("```", "").trim();
        return Map.of("mermaid", response);
    }
}