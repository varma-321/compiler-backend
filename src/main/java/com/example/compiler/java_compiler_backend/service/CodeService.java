package com.example.compiler.java_compiler_backend.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class CodeService {
    public Map<String, Object> runJavaCode(String code) {

        Map<String, Object> result = new HashMap<>();

        try {

            Path tempDir = Files.createTempDirectory("java_exec");
            Path javaFile = tempDir.resolve("Main.java");

            Files.writeString(javaFile, code);

            Process compile = new ProcessBuilder("javac", "Main.java")
                    .directory(tempDir.toFile())
                    .start();

            compile.waitFor();

            if (compile.exitValue() != 0) {

                String error = new String(compile.getErrorStream().readAllBytes());

                result.put("success", false);
                result.put("error", error);

                return result;
            }

            Process run = new ProcessBuilder("java", "Main")
                    .directory(tempDir.toFile())
                    .start();

            run.waitFor();

            String output = new String(run.getInputStream().readAllBytes());

            result.put("success", true);
            result.put("output", output);

        } catch (Exception e) {

            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    public Map<String, String> explainCode(String code) {

        String apiKey = System.getenv("GROQ_API_KEY");

        String prompt = "Explain this Java code step by step:\n" + code;

        try {

            URL url = new URL("https://api.groq.com/openai/v1/chat/completions");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");

            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");

            conn.setDoOutput(true);

            String json = """
            {
              "model": "llama-3.1-8b-instant",
              "messages":[
                {"role":"user","content":"%s"}
              ]
            }
            """.formatted(prompt);

            try(OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes());
            }

            InputStream stream;

            if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {
                stream = conn.getInputStream();
            } else {
                stream = conn.getErrorStream();
            }

            String response = new String(stream.readAllBytes());

            return Map.of("explanation", response);

        } catch (Exception e) {

            return Map.of("explanation", "AI Error: " + e.getMessage());
        }

    }
}
