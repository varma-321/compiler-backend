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

            String className = "Main";
            java.util.regex.Matcher pubMatcher = java.util.regex.Pattern
                    .compile("public\\s+class\\s+([A-Za-z0-9_]+)")
                    .matcher(executableCode);
            if (pubMatcher.find()) {
                className = pubMatcher.group(1);
            }

            Path file = dir.resolve(className + ".java");
            Files.writeString(file, executableCode);

            javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                // Fallback to ProcessBuilder if SystemJavaCompiler is unavailable
                Process compile = new ProcessBuilder("javac", "-g:none", className + ".java").directory(dir.toFile()).start();
                compile.waitFor();
                if (compile.exitValue() != 0) {
                    result.put("success", false);
                    result.put("error", new String(compile.getErrorStream().readAllBytes()));
                    return result;
                }
                Process run = new ProcessBuilder("java", "-XX:TieredStopAtLevel=1", "-cp", dir.toString(), className).directory(dir.toFile()).start();
                run.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
                result.put("success", run.exitValue() == 0);
                result.put("output", new String(run.getInputStream().readAllBytes()));
                return result;
            }

            ByteArrayOutputStream errStream = new ByteArrayOutputStream();
            int compRes = compiler.run(null, null, errStream, file.toString());
            if (compRes != 0) {
                result.put("success", false);
                result.put("error", errStream.toString());
                return result;
            }

            // Execute using reflection
            try (java.net.URLClassLoader classLoader = java.net.URLClassLoader.newInstance(new java.net.URL[]{dir.toUri().toURL()})) {
                Class<?> cls = Class.forName(className, true, classLoader);
                java.lang.reflect.Method mainMethod = cls.getMethod("main", String[].class);

                // Redirect System.out
                PrintStream originalOut = System.out;
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                PrintStream customOut = new PrintStream(outputStream);
                System.setOut(customOut);

                try {
                    mainMethod.invoke(null, (Object) new String[]{});
                } catch (java.lang.reflect.InvocationTargetException e) {
                    result.put("success", false);
                    result.put("error", e.getCause().toString());
                    System.setOut(originalOut);
                    return result;
                } finally {
                    System.setOut(originalOut);
                }

                result.put("success", true);
                result.put("output", outputStream.toString());
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }
}