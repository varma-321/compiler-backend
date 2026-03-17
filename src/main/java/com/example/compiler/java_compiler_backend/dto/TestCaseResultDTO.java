package com.example.compiler.java_compiler_backend.dto;

public class TestCaseResultDTO {
    private int test;
    private String status; // PASSED, FAILED
    private String expected;
    private String actual;
    private boolean isHidden;
    /** Exposed for failed cases so the frontend can show it like LeetCode */
    private String input;
    private String explanation;

    public TestCaseResultDTO() {}

    public int getTest() { return test; }
    public void setTest(int test) { this.test = test; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getExpected() { return expected; }
    public void setExpected(String expected) { this.expected = expected; }

    public String getActual() { return actual; }
    public void setActual(String actual) { this.actual = actual; }

    public boolean isHidden() { return isHidden; }
    public void setHidden(boolean hidden) { isHidden = hidden; }

    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
