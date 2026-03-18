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
import java.util.stream.Collectors;

@Service
public class SubmissionService {

    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final CodeExecutionService codeExecutionService;
    private final ProblemService problemService;
    private final ObjectMapper objectMapper;

    public SubmissionService(ProblemRepository problemRepository,
                             TestCaseRepository testCaseRepository,
                             CodeExecutionService codeExecutionService,
                             ProblemService problemService) {
        this.problemRepository = problemRepository;
        this.testCaseRepository = testCaseRepository;
        this.codeExecutionService = codeExecutionService;
        this.problemService = problemService;
        this.objectMapper = new ObjectMapper();
    }

    public SubmissionResponseDTO submitCode(String problemKey, String userCode, boolean runAllTests) throws Exception {
        try {
            Problem problem = problemService.getOrCreateProblem(problemKey, null);

            List<TestCase> testCases = runAllTests
                    ? testCaseRepository.findByProblemId(problem.getId())
                    : testCaseRepository.findByProblemIdAndIsHiddenFalse(problem.getId());

            if (testCases == null || testCases.isEmpty()) {
                throw new RuntimeException("No test cases found for problem ID: " + problem.getId());
            }

            Map<String, Object> methodSig = null;
            if (problem.getMethodSignature() != null && !problem.getMethodSignature().isEmpty()) {
                try {
                    methodSig = objectMapper.readValue(problem.getMethodSignature(), new TypeReference<Map<String, Object>>() {});
                } catch (Exception e) {
                    System.err.println("Error parsing method signature for problem " + problemKey + ": " + e.getMessage());
                }
            }

            final Map<String, Object> finalMethodSig = methodSig;

            // ── Execute All Test Cases in One Run ──
            long wallStart = System.currentTimeMillis();
            List<Map<String, String>> allInputs = new ArrayList<>();
            for (TestCase tc : testCases) {
                allInputs.add(parseInputs(tc.getInputs()));
            }

            String batchWrapped = buildBatchTestWrapper(userCode, allInputs, finalMethodSig);
            Map<String, Object> batchResult = codeExecutionService.runJava(batchWrapped);

            if (batchResult == null || !(Boolean) batchResult.get("success")) {
                SubmissionResponseDTO err = new SubmissionResponseDTO();
                err.setSuccess(false);
                err.setPassedTests(0);
                err.setTotalTests(testCases.size());
                String errorText = batchResult != null ? (String) batchResult.get("error") : "Execution failed";
                err.setStatus(errorText != null && errorText.contains("Exception") ? "RUNTIME_ERROR" : "COMPILATION_ERROR");
                err.setMessage(errorText);
                return err;
            }

            String fullOutput = (String) batchResult.get("output");
            String[] caseOutputs = fullOutput.split("---CASE_SEPARATOR---");

            List<TestCaseResultDTO> results = new ArrayList<>();
            int passed = 0;
            TestCaseResultDTO firstFailure = null;

            for (int i = 0; i < testCases.size(); i++) {
                TestCase tc = testCases.get(i);
                TestCaseResultDTO tr = new TestCaseResultDTO();
                tr.setTest(i + 1);
                tr.setExpected(tc.getExpectedOutput());
                tr.setHidden(tc.isHidden());

                String actual = (i < caseOutputs.length) ? caseOutputs[i].trim() : "";
                tr.setActual(actual);

                if (normalize(actual).equals(normalize(tc.getExpectedOutput()))) {
                    tr.setStatus("PASSED");
                    passed++;
                } else {
                    tr.setStatus("FAILED");
                    tr.setInput(allInputs.get(i).toString());
                    if (firstFailure == null) firstFailure = tr;
                }
                results.add(tr);
            }

            SubmissionResponseDTO response = new SubmissionResponseDTO();
            response.setPassedTests(passed);
            response.setTotalTests(testCases.size());
            response.setExecutionTimeMs(System.currentTimeMillis() - wallStart);

            if (firstFailure == null) {
                response.setSuccess(true);
                response.setStatus("ACCEPTED");
                response.setMessage("All " + passed + " test cases passed!");
                response.setResults(results.stream().filter(r -> !r.isHidden()).collect(Collectors.toList()));
            } else {
                response.setSuccess(false);
                response.setResults(results);
                response.setStatus("WRONG_ANSWER");
                response.setMessage("Failed on test " + firstFailure.getTest() + " — passed " + passed + "/" + testCases.size());
            }

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private String normalize(String s) {
        if (s == null) return "";
        // Remove all whitespace for robust comparison of arrays and objects
        return s.replaceAll("\\s+", "").toLowerCase();
    }

    private String buildBatchTestWrapper(String userCode, List<Map<String, String>> allInputs, Map<String, Object> methodSig) {
        String userClass = userCode.trim().replaceAll("^public\\s+class\\s+", "class ");
        StringBuilder casesBody = new StringBuilder();

        if (methodSig != null) {
            List<Map<String, String>> params = (List<Map<String, String>>) methodSig.get("params");
            String returnType = (String) methodSig.get("returnType");
            boolean isStatic = methodSig.get("isStatic") instanceof Boolean && (Boolean) methodSig.get("isStatic");
            String methodName = (String) methodSig.get("name");

            for (int i = 0; i < allInputs.size(); i++) {
                Map<String, String> inputs = allInputs.get(i);
                casesBody.append("        try {\n");
                List<String> argNames = new ArrayList<>();
                if (params != null) {
                    for (Map<String, String> param : params) {
                        String pType = param.get("type");
                        String pName = param.get("name");
                        String val = toJavaLiteral(inputs.getOrDefault(pName, getDefaultForType(pType)), pType);
                        casesBody.append(String.format("            %s %s = %s;\n", pType, pName, val));
                        argNames.add(pName);
                    }
                }

                String caller = isStatic ? "Solution." + methodName : "new Solution()." + methodName;
                if ("void".equals(returnType)) {
                    casesBody.append(String.format("            %s(%s);\n", caller, String.join(", ", argNames)));
                    casesBody.append("            System.out.println(\"void\");\n");
                } else {
                    casesBody.append("            Object result = ").append(caller).append("(").append(String.join(", ", argNames)).append(");\n");
                    casesBody.append("            if (result == null) System.out.println(\"null\");\n");
                    casesBody.append("            else if (result instanceof int[]) System.out.println(java.util.Arrays.toString((int[]) result));\n");
                    casesBody.append("            else if (result instanceof long[]) System.out.println(java.util.Arrays.toString((long[]) result));\n");
                    casesBody.append("            else if (result instanceof String[]) System.out.println(java.util.Arrays.toString((String[]) result));\n");
                    casesBody.append("            else if (result.getClass().isArray()) System.out.println(java.util.Arrays.deepToString((Object[]) result));\n");
                    casesBody.append("            else System.out.println(result);\n");
                }
                casesBody.append("        } catch (Exception e) { e.printStackTrace(); }\n");
                if (i < allInputs.size() - 1) {
                    casesBody.append("        System.out.println(\"---CASE_SEPARATOR---\");\n");
                }
            }
        }

        return "import java.util.*;\nimport java.io.*;\nimport java.util.Arrays;\n" + userClass + "\n" +
               "public class Main {\n" +
               "    public static void main(String[] args) {\n" +
               casesBody.toString() +
               "    }\n" +
               "}";
    }

    private String toJavaLiteral(String v, String type) {
        if (v == null || v.equals("null")) return "null";
        if (type == null) return "\"" + v + "\"";
        if (type.equals("String")) return "\"" + v.replace("\"", "\\\"") + "\"";
        if (type.equals("int[]")) return "new int[]{" + v.replace("[", "").replace("]", "") + "}";
        if (type.equals("long[]")) return "new long[]{" + v.replace("[", "").replace("]", "") + "}";
        return v;
    }

    private String getDefaultForType(String type) {
        if (type == null) return "null";
        if (type.equals("int")) return "0";
        if (type.equals("boolean")) return "false";
        return "null";
    }

    private Map<String, String> parseInputs(String json) throws Exception {
        if (json == null || json.isEmpty()) return new LinkedHashMap<>();
        Map<String, Object> raw = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        Map<String, String> result = new LinkedHashMap<>();
        if (raw != null) {
            for (Map.Entry<String, Object> e : raw.entrySet()) {
                result.put(e.getKey(), e.getValue() == null ? "null" : e.getValue().toString());
            }
        }
        return result;
    }
}
