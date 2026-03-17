package com.example.compiler.java_compiler_backend.dto;

import java.util.List;

public class ProblemResponseDTO {
    private String key;
    private String title;
    private String description;
    private String difficulty;
    private String topic;
    private String starterCode;
    private Object methodSignature; // Parsed JSON
    private Object hints; // Parsed JSON
    private String approach;
    private List<TestCaseDTO> testCases; // Only visible test cases

    public ProblemResponseDTO() {}

    // Getters and Setters
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getStarterCode() { return starterCode; }
    public void setStarterCode(String starterCode) { this.starterCode = starterCode; }

    public Object getMethodSignature() { return methodSignature; }
    public void setMethodSignature(Object methodSignature) { this.methodSignature = methodSignature; }

    public Object getHints() { return hints; }
    public void setHints(Object hints) { this.hints = hints; }

    public String getApproach() { return approach; }
    public void setApproach(String approach) { this.approach = approach; }

    public List<TestCaseDTO> getTestCases() { return testCases; }
    public void setTestCases(List<TestCaseDTO> testCases) { this.testCases = testCases; }
}
