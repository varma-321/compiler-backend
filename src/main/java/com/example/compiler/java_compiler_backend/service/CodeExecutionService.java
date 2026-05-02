package com.example.compiler.java_compiler_backend.service;

import org.springframework.stereotype.Service;

import javax.tools.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

@Service
public class CodeExecutionService {

    public Map<String, Object> runJava(String code, String stdin) {
        Map<String, Object> result = new HashMap<>();
        
        // Log for debugging (will appear in server console)
        System.out.println("--- NEW EXECUTION ---");
        System.out.println("Stdin: [" + (stdin == null ? "null" : stdin.replace("\n", "\\n")) + "]");

        try {
            String executableCode = code;
            if (!code.contains("public static void main")) {
                executableCode =
                    "import java.util.*;\n" +
                    "import java.io.*;\n" +
                    "import java.math.*;\n\n" +
                    code.trim().replaceAll("(?m)^\\s*public\\s+class\\s+", "class ") +
                    "\n\n" +
                    "public class Main {\n" +
                    "    public static void main(String[] args) {\n" +
                    "        System.out.println(\"Code compiled successfully. Use 'Run Tests' to execute against test cases.\");\n" +
                    "    }\n" +
                    "}\n";
            }

            Path dir = Files.createTempDirectory("java_exec_");
            String className = "Main";
            java.util.regex.Matcher pubMatcher = java.util.regex.Pattern
                    .compile("public\\s+class\\s+([A-Za-z0-9_]+)")
                    .matcher(executableCode);
            if (pubMatcher.find()) {
                className = pubMatcher.group(1);
            }

            Path file = dir.resolve(className + ".java");
            Files.writeString(file, executableCode, StandardCharsets.UTF_8);

            // ── Compile ─────────────────────────────────────────────────────
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler != null) {
                ByteArrayOutputStream errStream = new ByteArrayOutputStream();
                int compRes = compiler.run(null, null, errStream, file.toString());
                if (compRes != 0) {
                    result.put("success", false);
                    result.put("error", errStream.toString(StandardCharsets.UTF_8));
                    cleanup(dir);
                    return result;
                }
            } else {
                Process compile = new ProcessBuilder("javac", "-encoding", "UTF-8", className + ".java")
                        .directory(dir.toFile())
                        .redirectErrorStream(true)
                        .start();
                String output = new String(compile.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                if (!compile.waitFor(15, TimeUnit.SECONDS) || compile.exitValue() != 0) {
                    result.put("success", false);
                    result.put("error", output.isEmpty() ? "Compilation timed out or failed" : output);
                    cleanup(dir);
                    return result;
                }
            }

            // ── Execute ─────────────────────────────────────────────────────
            // Use -Dfile.encoding=UTF-8 to ensure Scanner handles the piped input correctly
            ProcessBuilder pb = new ProcessBuilder(
                    "java",
                    "-Dfile.encoding=UTF-8",
                    "-XX:TieredStopAtLevel=1",
                    "-cp", ".",
                    className
            );
            pb.directory(dir.toFile());
            
            Process run = pb.start();

            // Write STDIN immediately and close the stream to signal EOF
            if (stdin != null && !stdin.isEmpty()) {
                System.out.println("[Backend] Writing to stdin: " + stdin.replace("\n", "\\n"));
                try (OutputStream os = run.getOutputStream();
                     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
                    writer.write(stdin);
                    if (!stdin.endsWith("\n")) {
                        writer.newLine();
                    }
                    writer.flush();
                }
            } else {
                System.out.println("[Backend] Stdin is empty, closing stream.");
                run.getOutputStream().close();
            }

            // Read output streams using a more robust method
            ByteArrayOutputStream stdoutBuffer = new ByteArrayOutputStream();
            ByteArrayOutputStream stderrBuffer = new ByteArrayOutputStream();
            
            CompletableFuture<Void> outFuture = CompletableFuture.runAsync(() -> {
                try (InputStream is = run.getInputStream()) {
                    is.transferTo(stdoutBuffer);
                } catch (IOException ignored) {}
            });
            CompletableFuture<Void> errFuture = CompletableFuture.runAsync(() -> {
                try (InputStream is = run.getErrorStream()) {
                    is.transferTo(stderrBuffer);
                } catch (IOException ignored) {}
            });

            boolean finished = run.waitFor(15, TimeUnit.SECONDS);
            if (!finished) {
                run.destroyForcibly();
                result.put("success", false);
                result.put("error", "Execution timed out (15s limit).");
            } else {
                // Wait for output streams to be fully read
                try {
                    CompletableFuture.allOf(outFuture, errFuture).get(2, TimeUnit.SECONDS);
                } catch (Exception ignored) {}

                String stdout = stdoutBuffer.toString(StandardCharsets.UTF_8);
                String stderr = stderrBuffer.toString(StandardCharsets.UTF_8);

                if (run.exitValue() == 0) {
                    result.put("success", true);
                    result.put("output", stdout);
                    if (!stderr.isBlank()) {
                        result.put("stderr", stderr);
                    }
                } else {
                    result.put("stdin_received", stdin);
                    result.put("success", false);
                    // On runtime error, return stderr if available, otherwise stdout
                    result.put("error", stderr.isBlank() ? (stdout.isBlank() ? "Runtime Error (Exit Code " + run.exitValue() + ")" : stdout) : stderr);
                    if (!stdout.isBlank() && !stderr.isBlank()) {
                        result.put("output", stdout);
                    }
                }
            }

            cleanup(dir);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    public Map<String, Object> runJava(String code) {
        return runJava(code, null);
    }

    private void cleanup(Path dir) {
        try {
            Files.walk(dir)
                 .sorted(Comparator.reverseOrder())
                 .forEach(p -> {
                     try {
                         Files.delete(p);
                     } catch (IOException ignored) {}
                 });
        } catch (IOException ignored) {}
    }
}