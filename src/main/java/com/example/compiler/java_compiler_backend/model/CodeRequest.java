package com.example.compiler.java_compiler_backend.model;

public class CodeRequest {

    private String code;

    public CodeRequest() {
    }

    public CodeRequest(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}