package com.example.compiler.java_compiler_backend.service;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@Service
public class CodeExecutionService {

    /**
     * Runs raw Java code. If the code doesn't contain a main() method we wrap it
     * in a minimal harness so it can be compiled and run. This mirrors the "Run"
     * button behaviour that just compiles and executes the user's code for a
     * quick sanity‑check, as opposed to the full test‑harness built by the
     * SubmissionService.
     */
    public Map<String, Object> runJava(String code) {
        Map<String, Object> result = new HashMap<>();

        try {
            // If the user hasn't written a main method, wrap the class in a
            // simple Main stub so javac/java don't complain.
            String executableCode = code;
            if (!code.contains("public static void main")) {
                // Extract the class body and embed as inner class inside Main
                // so the user's Solution class is still accessible.
                executableCode =
                    "import java.util.*;\n" +
                    "import java.io.*;\n" +
                    "import java.math.*;\n\n" +
                    code.trim().replaceAll("^public\\s+class\\s+", "class ") +
                    "\n\n" +
                    "public class Main {\n" +
                    "    public static void main(String[] args) {\n" +
                    "        System.out.println(\"Code compiled successfully. Use 'Run Tests' to execute against test cases.\");\n" +
                    "    }\n" +
                    "}\n";
            }

            Path dir = Files.createTempDirectory("java_exec");

            // Find the PUBLIC class in the final code — that must match the file name.
            // The SubmissionService harness generates "public class Main", so we must
            // look at executableCode (not the raw user code) to find the right name.
            String className = "Main";
            java.util.regex.Matcher pubMatcher = java.util.regex.Pattern
                    .compile("public\\s+class\\s+([A-Za-z0-9_]+)")
                    .matcher(executableCode);
            if (pubMatcher.find()) {
                className = pubMatcher.group(1);
            }

            Path file = dir.resolve(className + ".java");
            Files.writeString(file, executableCode);

            Process compile = new ProcessBuilder("javac", className + ".java")
                    .directory(dir.toFile())
                    .redirectErrorStream(false)
                    .start();

            compile.waitFor();

            if (compile.exitValue() != 0) {
                String error = new String(compile.getErrorStream().readAllBytes());
                result.put("success", false);
                result.put("error", error);
                return result;
            }

            Process run = new ProcessBuilder("java", "-cp", dir.toString(), className)
                    .directory(dir.toFile())
                    .redirectErrorStream(true)
                    .start();

            // Time‑limit: 10 seconds
            boolean finished = run.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                run.destroyForcibly();
                result.put("success", false);
                result.put("error", "Time Limit Exceeded (10s)");
                return result;
            }

            String output = new String(run.getInputStream().readAllBytes());

            result.put("success", true);
            result.put("output", output);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }
}