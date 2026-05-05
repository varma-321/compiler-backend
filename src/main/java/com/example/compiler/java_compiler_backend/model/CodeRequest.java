package com.example.compiler.java_compiler_backend.model;

public class CodeRequest {

    private String code;
    private Integer hintLevel;
    private String type;
    private String problemId;
    private String problemTitle;
    private String inputs;
    private String stdin;
    private String existingTestCases; // JSON string of already existing test cases for dedup

    public CodeRequest() {
    }

    public CodeRequest(String code) {
        this.code = code;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Integer getHintLevel() { return hintLevel; }
    public void setHintLevel(Integer hintLevel) { this.hintLevel = hintLevel; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getProblemId() { return problemId; }
    public void setProblemId(String problemId) { this.problemId = problemId; }

    public String getProblemTitle() { return problemTitle; }
    public void setProblemTitle(String problemTitle) { this.problemTitle = problemTitle; }

    public String getInputs() { return inputs; }
    public void setInputs(String inputs) { this.inputs = inputs; }

    public String getStdin() { return stdin; }
    public void setStdin(String stdin) { this.stdin = stdin; }

    public String getExistingTestCases() { return existingTestCases; }
    public void setExistingTestCases(String existingTestCases) { this.existingTestCases = existingTestCases; }
}