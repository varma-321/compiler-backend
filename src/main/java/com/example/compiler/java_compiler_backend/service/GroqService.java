package com.example.compiler.java_compiler_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class GroqService {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    public String askAI(String prompt) throws Exception {

        String apiKey = System.getenv("GROQ_API_KEY");

        URL url = new URL(API_URL);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");

        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");

        conn.setDoOutput(true);

        prompt = prompt.replace("\"", "\\\"").replace("\n", "\\n");

        String body = """
        {
          "model": "llama-3.1-8b-instant",
          "messages":[
            {"role":"user","content":"%s"}
          ]
        }
        """.formatted(prompt);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes());
        }

        InputStream stream = conn.getResponseCode() < 300
                ? conn.getInputStream()
                : conn.getErrorStream();

        String response = new String(stream.readAllBytes());

        ObjectMapper mapper = new ObjectMapper();

        JsonNode root = mapper.readTree(response);

        String content = root
                .path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText();

        content = content
                .replace("```json", "")
                .replace("```", "")
                .trim();

        return content;
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
}