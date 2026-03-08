package com.example.compiler.java_compiler_backend.service;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.*;
import java.util.*;

@Service
public class GroqService {

    private final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

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
          "messages": [
            {"role": "user", "content": "%s"}
          ]
        }
        """.formatted(prompt);

        try(OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes());
        }

        InputStream stream = conn.getResponseCode() < 300 ?
                conn.getInputStream() :
                conn.getErrorStream();

        return new String(stream.readAllBytes());
    }
}