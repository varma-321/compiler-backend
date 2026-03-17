package com.example.compiler.java_compiler_backend.dto;

import java.util.List;

public class SubmissionResponseDTO {
    private boolean success;
    private String status; // ACCEPTED, WRONG_ANSWER, COMPILATION_ERROR, RUNTIME_ERROR
    private String message;
    private int passedTests;
    private int totalTests;
    private long executionTimeMs;
    private List<TestCaseResultDTO> results;

    public SubmissionResponseDTO() {}

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getPassedTests() { return passedTests; }
    public void setPassedTests(int passedTests) { this.passedTests = passedTests; }

    public int getTotalTests() { return totalTests; }
    public void setTotalTests(int totalTests) { this.totalTests = totalTests; }

    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public List<TestCaseResultDTO> getResults() { return results; }
    public void setResults(List<TestCaseResultDTO> results) { this.results = results; }
}
