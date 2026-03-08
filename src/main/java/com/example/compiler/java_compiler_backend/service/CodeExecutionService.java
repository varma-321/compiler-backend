package com.example.compiler.java_compiler_backend.service;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@Service
public class CodeExecutionService {

    public Map<String, Object> runJava(String code) {

        Map<String, Object> result = new HashMap<>();

        try {

            Path dir = Files.createTempDirectory("java_exec");
            Path file = dir.resolve("Main.java");

            Files.writeString(file, code);

            Process compile = new ProcessBuilder("javac", "Main.java")
                    .directory(dir.toFile())
                    .start();

            compile.waitFor();

            if (compile.exitValue() != 0) {
                String error = new String(compile.getErrorStream().readAllBytes());
                result.put("success", false);
                result.put("error", error);
                return result;
            }

            Process run = new ProcessBuilder("java", "Main")
                    .directory(dir.toFile())
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
}