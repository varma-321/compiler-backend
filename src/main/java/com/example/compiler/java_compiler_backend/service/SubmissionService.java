package com.example.compiler.java_compiler_backend.service;

import com.example.compiler.java_compiler_backend.dto.SubmissionResponseDTO;
import com.example.compiler.java_compiler_backend.dto.TestCaseResultDTO;
import com.example.compiler.java_compiler_backend.model.Problem;
import com.example.compiler.java_compiler_backend.model.TestCase;
import com.example.compiler.java_compiler_backend.repository.ProblemRepository;
import com.example.compiler.java_compiler_backend.repository.TestCaseRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class SubmissionService {

    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final CodeExecutionService codeExecutionService;
    private final ObjectMapper objectMapper;

    // Thread pool: one thread per logical CPU, cap at 8 for safety
    private final ExecutorService pool = Executors.newFixedThreadPool(
        Math.min(Runtime.getRuntime().availableProcessors(), 8)
    );

    public SubmissionService(ProblemRepository problemRepository,
                             TestCaseRepository testCaseRepository,
                             CodeExecutionService codeExecutionService) {
        this.problemRepository = problemRepository;
        this.testCaseRepository = testCaseRepository;
        this.codeExecutionService = codeExecutionService;
        this.objectMapper = new ObjectMapper();
    }

    public SubmissionResponseDTO submitCode(String problemKey, String userCode, boolean runAllTests) throws Exception {
        Problem problem = problemRepository.findByKey(problemKey)
                .orElseThrow(() -> new RuntimeException("Problem not found: " + problemKey));

        List<TestCase> testCases = runAllTests
                ? testCaseRepository.findByProblemId(problem.getId())
                : testCaseRepository.findByProblemIdAndIsHiddenFalse(problem.getId());

        if (testCases.isEmpty()) {
            throw new RuntimeException("No test cases found for problem.");
        }

        Map<String, Object> methodSig = null;
        if (problem.getMethodSignature() != null) {
            methodSig = objectMapper.readValue(problem.getMethodSignature(), new TypeReference<>() {});
        }

        final Map<String, Object> finalMethodSig = methodSig;

        // ── Compile ONCE (dry-run with first test) to catch compilation errors fast ────
        // We do this before spinning up the thread pool so we don't waste threads on
        // definitely-bad code.
        {
            // ── Compile ONCE (dry-run with first test) ─────────────────────────────
            Map<String, String> firstInputs = parseInputs(testCases.get(0).getInputs());
            String probe = buildTestWrapper(userCode, problem.getTitle(), firstInputs, finalMethodSig);
            Map<String, Object> probeResult = codeExecutionService.runJava(probe);
            if (!(Boolean) probeResult.get("success")) {
                // Compilation / runtime error: return immediately
                SubmissionResponseDTO err = new SubmissionResponseDTO();
                err.setSuccess(false);
                err.setPassedTests(0);
                err.setTotalTests(testCases.size());
                err.setExecutionTimeMs(0L);
                String errorText = (String) probeResult.get("error");
                err.setStatus(errorText != null && errorText.contains("Exception") ? "RUNTIME_ERROR" : "COMPILATION_ERROR");
                err.setMessage(errorText != null ? errorText : "Compilation failed");

                TestCaseResultDTO tr = new TestCaseResultDTO();
                tr.setTest(1);
                tr.setExpected(testCases.get(0).getExpectedOutput());
                tr.setHidden(testCases.get(0).isHidden());
                tr.setStatus("FAILED");
                tr.setActual("Error:\n" + errorText);
                // Show input for failed visible case
                if (!testCases.get(0).isHidden()) {
                    tr.setInput(firstInputs.toString());
                }
                err.setResults(List.of(tr));
                return err;
            }
        }

        // ── Run all test cases in parallel ────────────────────────────────────────────
        long wallStart = System.currentTimeMillis();

        List<Future<TestCaseResultDTO>> futures = new ArrayList<>();
        for (int i = 0; i < testCases.size(); i++) {
            final int idx = i;
            final TestCase tc = testCases.get(i);
            futures.add(pool.submit(() -> {
                try {
                    // ── Robustly parse inputs: values may be JSON strings or raw JSON arrays
                    Map<String, String> inputsMap = parseInputs(tc.getInputs());
                    String wrapped = buildTestWrapper(userCode, problem.getTitle(), inputsMap, finalMethodSig);
                    Map<String, Object> result = codeExecutionService.runJava(wrapped);

                    TestCaseResultDTO tr = new TestCaseResultDTO();
                    tr.setTest(idx + 1);
                    tr.setExpected(tc.getExpectedOutput());
                    tr.setHidden(tc.isHidden());

                    if (!(Boolean) result.get("success")) {
                        tr.setStatus("FAILED");
                        tr.setActual("Runtime Error:\n" + result.get("error"));
                        if (!tc.isHidden()) {
                            tr.setInput(inputsMap.toString());
                            tr.setExplanation(tc.getExplanation());
                        }
                    } else {
                        String actual = ((String) result.get("output")).trim();
                        String expected = tc.getExpectedOutput().trim();
                        tr.setActual(actual);
                        if (normalize(actual).equals(normalize(expected))) {
                            tr.setStatus("PASSED");
                        } else {
                            tr.setStatus("FAILED");
                            // Always expose input/explanation for failed cases (visible or hidden)
                            tr.setInput(inputsMap.toString());
                            if (tc.getExplanation() != null) tr.setExplanation(tc.getExplanation());
                        }
                    }
                    return tr;
                } catch (Exception e) {
                    TestCaseResultDTO tr = new TestCaseResultDTO();
                    tr.setTest(idx + 1);
                    tr.setExpected(tc.getExpectedOutput());
                    tr.setHidden(tc.isHidden());
                    tr.setStatus("FAILED");
                    tr.setActual("Error: " + e.getMessage());
                    return tr;
                }
            }));
        }

        // Collect results in order
        List<TestCaseResultDTO> results = new ArrayList<>();
        int passed = 0;
        TestCaseResultDTO firstFailure = null;

        for (Future<TestCaseResultDTO> future : futures) {
            TestCaseResultDTO tr = future.get(30, TimeUnit.SECONDS);
            results.add(tr);
            if ("PASSED".equals(tr.getStatus())) {
                passed++;
            } else if (firstFailure == null) {
                firstFailure = tr;
            }
        }

        long wallTime = System.currentTimeMillis() - wallStart;

        // ── Build response ────────────────────────────────────────────────────────────
        SubmissionResponseDTO response = new SubmissionResponseDTO();
        response.setPassedTests(passed);
        response.setTotalTests(testCases.size());
        response.setExecutionTimeMs(wallTime);

        if (firstFailure == null) {
            response.setSuccess(true);
            response.setStatus("ACCEPTED");
            response.setMessage("All " + passed + " test cases passed!");
            // Return only visible results when all pass (don't leak hidden inputs)
            response.setResults(results.stream().filter(r -> !r.isHidden()).collect(Collectors.toList()));
        } else {
            response.setSuccess(false);
            // Return ALL results so the UI can show which ones failed, plus first failure details
            response.setResults(results);

            String failActual = firstFailure.getActual() != null ? firstFailure.getActual() : "";
            if (failActual.contains("Runtime Error") || failActual.contains("Exception")) {
                response.setStatus("RUNTIME_ERROR");
            } else {
                response.setStatus("WRONG_ANSWER");
            }
            response.setMessage("Failed on test " + firstFailure.getTest() +
                    " — passed " + passed + "/" + testCases.size());
        }

        return response;
    }

    // ─── Normalization ────────────────────────────────────────────────────────────

    private String normalize(String s) {
        String trimmed = s.trim();
        if (trimmed.matches("^\\[[\\s\\S]*\\]$")) {
            String noOuterSpace = trimmed.replaceAll("\\s*\\[\\s*", "[").replaceAll("\\s*\\]\\s*", "]");
            String compactCommas = noOuterSpace.replaceAll(",\\s+", ",");
            return compactCommas.toLowerCase();
        }
        return trimmed.replaceAll("\\s+", " ").toLowerCase();
    }

    // ─── Test wrapper builder ─────────────────────────────────────────────────────

    private String buildTestWrapper(String userCode, String problemTitle,
                                    Map<String, String> inputs, Map<String, Object> methodSig) {
        String userClass = userCode.trim().replaceAll("^public\\s+class\\s+", "class ");

        StringBuilder varDecls = new StringBuilder();
        StringBuilder methodCall = new StringBuilder();

        if (methodSig != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> params = (List<Map<String, String>>) methodSig.get("params");
            String returnType = (String) methodSig.get("returnType");
            Object isStaticObj = methodSig.get("isStatic");
            boolean isStatic = isStaticObj instanceof Boolean ? (Boolean) isStaticObj : false;
            String methodName = (String) methodSig.get("name");

            List<String> argNames = new ArrayList<>();
            for (Map<String, String> param : params) {
                String pType = param.get("type");
                String pName = param.get("name");
                String val = inputs.getOrDefault(pName, getDefaultForType(pType));
                val = toJavaLiteral(val, pType);
                varDecls.append(String.format("            %s %s = %s;\n", pType, pName, val));
                argNames.add(pName);
            }

            String argsStr = String.join(", ", argNames);
            String caller = isStatic ? "Solution." + methodName : "new Solution()." + methodName;

            if ("void".equals(returnType)) {
                methodCall.append(String.format("            %s(%s);\n", caller, argsStr));
                methodCall.append("            System.out.println(\"void\");\n");
            } else {
                methodCall.append(String.format("            %s result = %s(%s);\n", returnType, caller, argsStr));
                methodCall.append("            ").append(buildOutputPrint(returnType)).append("\n");
            }
        } else {
            methodCall.append("            System.out.println(\"ERROR: Method signature missing.\");\n");
        }

        return "import java.util.*;\nimport java.io.*;\nimport java.math.*;\n" +
                userClass + "\n\n" +
                "public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        try {\n" +
                varDecls +
                methodCall +
                "        } catch (Exception e) {\n" +
                "            System.out.println(\"ERROR: \" + e.getMessage());\n" +
                "            e.printStackTrace();\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
    }

    private String toJavaLiteral(String v, String javaType) {
        v = v.trim();
        return switch (javaType) {
            case "int" -> {
                try { yield String.valueOf(Integer.parseInt(v)); } 
                catch (NumberFormatException e) { yield "0"; }
            }
            case "double" -> {
                try { yield String.valueOf(Double.parseDouble(v)); }
                catch (NumberFormatException e) { yield "0.0"; }
            }
            case "boolean" -> v.equals("true") ? "true" : "false";
            case "long" -> v.endsWith("L") ? v : v.replaceAll("[^0-9\\-]", "") + "L";
            case "float" -> v.endsWith("f") ? v : v + "f";
            case "String" -> v.startsWith("\"") && v.endsWith("\"") ? v : "\"" + v.replace("\"", "\\\"") + "\"";
            case "int[]" -> {
                // Input like "[5,4,3,2,1]" or "5,4,3,2,1" -> new int[]{5,4,3,2,1}
                String inner = v.startsWith("[") ? v.substring(1, v.length() - 1) : v;
                yield "new int[]{" + inner + "}";
            }
            case "long[]" -> {
                String inner = v.startsWith("[") ? v.substring(1, v.length() - 1) : v;
                yield "new long[]{" + inner + "L" + "}";
            }
            case "double[]" -> {
                String inner = v.startsWith("[") ? v.substring(1, v.length() - 1) : v;
                yield "new double[]{" + inner + "}";
            }
            case "String[]" -> {
                String inner = v.startsWith("[") ? v.substring(1, v.length() - 1) : v;
                // Wrap each element in quotes if not already
                String[] parts = inner.split(",");
                StringBuilder sb = new StringBuilder("new String[]{");
                for (int i = 0; i < parts.length; i++) {
                    String p = parts[i].trim().replaceAll("\"", "");
                    sb.append("\"").append(p).append("\"");
                    if (i < parts.length - 1) sb.append(", ");
                }
                sb.append("}");
                yield sb.toString();
            }
            case "int[][]" -> {
                // Input like "[[1,2],[3,4]]" -> new int[][]{{1,2},{3,4}}
                yield "new int[][]" + v.replace("[", "{").replace("]", "}");
            }
            case "List<Integer>" -> {
                String inner = v.startsWith("[") ? v.substring(1, v.length() - 1) : v;
                yield "new java.util.ArrayList<>(java.util.Arrays.asList(" + 
                      java.util.Arrays.stream(inner.split(",")).map(s -> s.trim()).collect(java.util.stream.Collectors.joining(",")) +
                      "))";
            }
            case "List<String>" -> {
                String inner = v.startsWith("[") ? v.substring(1, v.length() - 1) : v;
                String[] parts = inner.split(",");
                StringBuilder sb = new StringBuilder("new java.util.ArrayList<>(java.util.Arrays.asList(");
                for (int i = 0; i < parts.length; i++) {
                    String p = parts[i].trim().replaceAll("\"", "");
                    sb.append("\"").append(p).append("\"");
                    if (i < parts.length - 1) sb.append(", ");
                }
                sb.append("))");
                yield sb.toString();
            }
            default -> {
                // For TreeNode, ListNode etc. just pass null or the value as-is
                if (v.equals("null")) yield "null";
                if (v.startsWith("[")) yield "null"; // Tree/Linked List structures unsupported in raw executor
                if (v.startsWith("\"") && v.endsWith("\"")) yield v;
                yield "\"" + v + "\"";
            }
        };
    }

    private String getDefaultForType(String javaType) {
        if (javaType.contains("[]")) return "new " + javaType + "{}";
        return switch (javaType) {
            case "int" -> "0";
            case "long" -> "0L";
            case "double" -> "0.0";
            case "boolean" -> "false";
            case "String" -> "\"\"";
            default -> "null";
        };
    }

    private String buildOutputPrint(String returnType) {
        if (returnType.endsWith("[]") && !returnType.endsWith("[][]")) {
            return "System.out.println(java.util.Arrays.toString(result));";
        }
        if (returnType.endsWith("[][]")) {
            return "System.out.println(java.util.Arrays.deepToString(result));";
        }
        return "System.out.println(result);";
    }
    /**
     * Robustly convert stored test-case inputs JSON to Map<String,String>.
     * The AI may store values as quoted strings OR as raw JSON arrays/objects.
     * We stringify everything so the downstream code always gets a plain String.
     */
    private Map<String, String> parseInputs(String json) throws Exception {
        Map<String, Object> raw = objectMapper.readValue(json, new TypeReference<>() {});
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : raw.entrySet()) {
            Object v = e.getValue();
            if (v == null) {
                result.put(e.getKey(), "null");
            } else if (v instanceof String) {
                result.put(e.getKey(), (String) v);
            } else {
                // List, Integer, Boolean, etc. — serialize back to JSON-like string
                result.put(e.getKey(), objectMapper.writeValueAsString(v));
            }
        }
        return result;
    }
}
