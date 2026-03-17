package com.example.compiler.java_compiler_backend.dto;

public class TestCaseDTO {
    private Object inputs; // Parsed JSON map
    private String expectedOutput;
    private String explanation;

    public TestCaseDTO() {}

    public Object getInputs() { return inputs; }
    public void setInputs(Object inputs) { this.inputs = inputs; }

    public String getExpectedOutput() { return expectedOutput; }
    public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
