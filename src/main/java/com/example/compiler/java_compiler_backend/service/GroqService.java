package com.example.compiler.java_compiler_backend.service;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

@Service
public class GroqService {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    public String analyzeCode(String code) throws Exception {

        String apiKey = System.getenv("GROQ_API_KEY");

        String prompt = """
        You are a Java DSA expert.

        Analyze the following Java code and return ONLY valid JSON in this format:

        {
          "problemName": "name of the algorithm or problem",
          "algorithmUsed": "algorithm or technique used",
          "timeComplexity": "Big-O time complexity",
          "spaceComplexity": "Big-O space complexity",
          "summary": "short explanation of what the code does",
          "optimizations": ["optimization 1","optimization 2"]
        }

        Rules:
        - Return ONLY JSON
        - Do not include markdown
        - Do not include explanation outside JSON

        Java code:
        """ + code;

        prompt = prompt.replace("\"", "\\\"").replace("\n", "\\n");

        URL url = new URL(API_URL);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

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

        InputStream responseStream = conn.getResponseCode() < 300
                ? conn.getInputStream()
                : conn.getErrorStream();

        Scanner scanner = new Scanner(responseStream).useDelimiter("\\A");
        String response = scanner.hasNext() ? scanner.next() : "";

        return response;
    }

    public String simplePrompt(String promptText) throws Exception {

        String apiKey = System.getenv("GROQ_API_KEY");

        promptText = promptText.replace("\"", "\\\"").replace("\n", "\\n");

        URL url = new URL(API_URL);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String body = """
        {
          "model": "llama-3.1-8b-instant",
          "messages":[
            {"role":"user","content":"%s"}
          ]
        }
        """.formatted(promptText);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes());
        }

        InputStream responseStream = conn.getResponseCode() < 300
                ? conn.getInputStream()
                : conn.getErrorStream();

        Scanner scanner = new Scanner(responseStream).useDelimiter("\\A");
        String response = scanner.hasNext() ? scanner.next() : "";

        return response;
    }
}