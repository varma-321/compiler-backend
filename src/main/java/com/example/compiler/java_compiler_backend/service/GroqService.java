package com.example.compiler.java_compiler_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class GroqService {

    private final String apiKey;
    private final ObjectMapper mapper;

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    public GroqService(@org.springframework.beans.factory.annotation.Value("${GROQ_API_KEY:}") String apiKey) {
        this.apiKey = apiKey;
        this.mapper = new ObjectMapper();
    }

    public String askAI(String prompt) throws Exception {
        return askAI(null, prompt, "llama-3.1-8b-instant");
    }

    public String askAI(String systemPrompt, String userPrompt) throws Exception {
        return askAI(systemPrompt, userPrompt, "llama-3.1-8b-instant");
    }

    public String askAI(String systemPrompt, String userPrompt, String model) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("GROQ_API_KEY is not configured. Please set it in application.properties or as an environment variable.");
        }

        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("model", model);
        bodyMap.put("temperature", 0.2);
        bodyMap.put("max_tokens", 4096);

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt != null ? systemPrompt : "You are an elite Java DSA Architect and Mentor. Provide clean, professional, and efficient Java-centric solutions. Always prioritize modern Java (JDK 17+) idioms.");
        messages.add(systemMsg);

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        messages.add(userMsg);

        bodyMap.put("messages", messages);

        byte[] bodyBytes = mapper.writeValueAsBytes(bodyMap);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(bodyBytes);
        }

        InputStream stream = conn.getResponseCode() < 300
                ? conn.getInputStream()
                : conn.getErrorStream();

        String response = new String(stream.readAllBytes());
        JsonNode root = mapper.readTree(response);

        if (root.has("error")) {
            throw new RuntimeException("Groq API Error: " + root.path("error").path("message").asText());
        }

        String content = root
                .path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText();

        // Robust cleaning
        content = content.trim();
        if (content.startsWith("```json")) {
            content = content.substring(7);
        } else if (content.startsWith("```")) {
            content = content.substring(3);
        }
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }

        return content.trim();
    }

    public String analyzeCode(String code) throws Exception {

        String prompt = """
        You are a Java DSA expert.

        Analyze the following Java code and return ONLY JSON in this format:

        {
          "problemName": "name of the algorithm or problem",
          "algorithmUsed": "algorithm or technique used",
          "timeComplexity": "Big-O time complexity",
          "spaceComplexity": "Big-O space complexity",
          "summary": "short explanation",
          "optimizations": []
        }

        Java code:
        """ + code;

        return askAI(prompt);
    }

    public String generateProblemData(String key, String title) throws Exception {
        String prompt = """
        Generate a professional Java DSA problem for:
        Key: %s
        Title: %s
        
        OUTPUT FORMAT:
        Return ONLY valid minified JSON. No conversational text.
        
        CONSTRAINTS:
        1. 'starterCode' MUST contain the official LeetCode-style Java class and method.
        2. 'methodSignature' MUST exactly match the starter code (name, returnType, params).
        3. 'params' in 'methodSignature' must be an array of objects: {"type": "...", "name": "..."}.
        4. 'testCases' must have EXACTLY 3 visible and 2 hidden cases.
        5. For problems like "Evaluate Reverse Polish Notation", use the standard signature: public int evalRPN(String[] tokens).
        6. All newlines inside strings MUST be escaped as \\n.
        7. CRITICAL: NEVER use "null" or any reserved keywords as a method name. If the method name is unclear or unavailable, default to "solve".
        
        STRUCTURE:
        {
          "title": "...",
          "description": "...",
          "difficulty": "Easy/Medium/Hard",
          "topic": "...",
          "starterCode": "class Solution {\\n    public [ReturnType] [MethodName]([Params]) {\\n        \\n    }\\n}",
          "methodSignature": {"name": "...", "returnType": "...", "params": [{"type": "...", "name": "..."}], "isStatic": false},
          "hints": ["..."],
          "approach": "...",
          "testCases": [{"inputs": {"paramName": "value"}, "expectedOutput": "...", "isHidden": false}]
        }
        """.formatted(key, title);

        return askAIWithModel("llama-3.3-70b-versatile", "You are an elite Java DSA JSON generator. You provide official signatures for all standard problems.", prompt);
    }
    
    private String askAIWithModel(String model, String system, String user) throws Exception {
        return askAI(system, user, model);
    }

    public String dryRun(String code, String inputs) throws Exception {
        String system = "You are a Java Execution Simulator. Trace the following code with the provided inputs step-by-step. Focus on state changes and loop iterations.";
        String prompt = "Code:\n" + code + "\n\nInputs: " + inputs + "\n\nPerform a step-by-step dry run.";
        return askAI(system, prompt);
    }

    public String analyzeComplexityAdvanced(String code) throws Exception {
        String system = "You are a Java Performance Engineer. Analyze the time and space complexity in extreme detail. Return Markdown with a 'Complexity Radar' table.";
        String prompt = "Analyze this Java code for Big-O efficiency and potential bottlenecks:\n" + code;
        return askAI(system, prompt);
    }
}