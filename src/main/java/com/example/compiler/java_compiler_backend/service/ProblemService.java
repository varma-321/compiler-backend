package com.example.compiler.java_compiler_backend.service;

import com.example.compiler.java_compiler_backend.dto.ProblemResponseDTO;
import com.example.compiler.java_compiler_backend.dto.TestCaseDTO;
import com.example.compiler.java_compiler_backend.model.Problem;
import com.example.compiler.java_compiler_backend.model.TestCase;
import com.example.compiler.java_compiler_backend.repository.ProblemRepository;
import com.example.compiler.java_compiler_backend.repository.TestCaseRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final GroqService groqService;
    private final ObjectMapper objectMapper;

    public ProblemService(ProblemRepository problemRepository, TestCaseRepository testCaseRepository, GroqService groqService) {
        this.problemRepository = problemRepository;
        this.testCaseRepository = testCaseRepository;
        this.groqService = groqService;
        this.objectMapper = new ObjectMapper();
    }

    public ProblemResponseDTO getProblemByKey(String key, String title) throws Exception {
        Problem problem = getOrCreateProblem(key, title);


        List<TestCase> visibleCases = testCaseRepository.findByProblemIdAndIsHiddenFalse(problem.getId());

        ProblemResponseDTO dto = new ProblemResponseDTO();
        dto.setKey(problem.getKey());
        dto.setTitle(problem.getTitle());
        dto.setDescription(problem.getDescription());
        dto.setDifficulty(problem.getDifficulty());
        dto.setTopic(problem.getTopic());
        dto.setStarterCode(problem.getStarterCode());
        dto.setApproach(problem.getApproach());

        if (problem.getMethodSignature() != null) {
            dto.setMethodSignature(objectMapper.readValue(problem.getMethodSignature(), Object.class));
        }
        if (problem.getHints() != null) {
            dto.setHints(objectMapper.readValue(problem.getHints(), Object.class));
        }

        List<TestCaseDTO> tcDtos = visibleCases.stream().map(tc -> {
            TestCaseDTO td = new TestCaseDTO();
            td.setExpectedOutput(tc.getExpectedOutput());
            td.setExplanation(tc.getExplanation());
            try {
                td.setInputs(objectMapper.readValue(tc.getInputs(), Object.class));
            } catch (JsonProcessingException e) {
                td.setInputs(tc.getInputs());
            }
            return td;
        }).collect(Collectors.toList());
        dto.setTestCases(tcDtos);

        return dto;
    }

    @Transactional
    public Problem getOrCreateProblem(String key, String title) throws Exception {
        Problem problem = problemRepository.findByKey(key)
                .or(() -> {
                    String cleanTitle = key.replaceAll("-", " ").trim();
                    return problemRepository.findAll().stream()
                            .filter(p -> p.getTitle().equalsIgnoreCase(cleanTitle) || p.getKey().contains(key) || key.contains(p.getKey()))
                            .findFirst();
                })
                .orElse(null);

        if (problem == null) {
            String sanitizedTitle = (title != null && !title.isEmpty()) ? title : key.replaceAll("-", " ");
            problem = generateAndSaveProblem(key, sanitizedTitle);
        }
        return problem;
    }

    @PostConstruct
    @Transactional
    public void seedDatabase() {
        // Force re-seed of problems with empty signatures
        List<Problem> emptySigProblems = problemRepository.findAll().stream()
            .filter(p -> p.getMethodSignature() != null && p.getMethodSignature().equals("{}"))
            .collect(Collectors.toList());
        if (!emptySigProblems.isEmpty()) {
            problemRepository.deleteAll(emptySigProblems);
            System.out.println("🔧 Deleted " + emptySigProblems.size() + " problems with '{}' signatures to reseed them.");
        }

        seedTwoSum();
        seedContainsDuplicate();
        seedValidAnagram();
        seedValidPalindrome();
        seedMaximumConsecutiveOnes();
        seedFindMissingNumber();
        seedMoveZeroes();
        seedIntersectionOfTwoSortedArrays();
        seedUnionOfTwoSortedArrays();
        seedLargestElementInArray();
        seedSecondLargestElement();
        seedCheckIfArrayIsSorted();
        seedRemoveDuplicatesFromSortedArray();
        seedStockBuyAndSell();
        seedKadanesAlgorithm();
        seedMajorityElementN2();
        
        // Auto-repair existing broken problems where the AI generated "null" as the method name
        List<Problem> badProblems = problemRepository.findAll().stream()
            .filter(p -> p.getMethodSignature() != null && p.getMethodSignature().contains("\"name\":\"null\""))
            .collect(Collectors.toList());
            
        for (Problem p : badProblems) {
            p.setMethodSignature(p.getMethodSignature().replace("\"name\":\"null\"", "\"name\":\"solve\""));
            if (p.getStarterCode() != null) {
                p.setStarterCode(p.getStarterCode().replace(" null(", " solve(").replace(" null (", " solve("));
            }
        }
        if (!badProblems.isEmpty()) {
            problemRepository.saveAll(badProblems);
            System.out.println("🔧 Auto-repaired " + badProblems.size() + " problems with 'null' method names.");
        }
        
        System.out.println("✅ Database seed and repair complete.");
    }

    private Problem newProblem(String key, String title, String difficulty, String topic,
                               String description, String starterCode, String methodSig,
                               String hints, String approach) {
        if (problemRepository.findByKey(key).isPresent()) return null;
        Problem p = new Problem();
        p.setKey(key);
        p.setTitle(title);
        p.setDifficulty(difficulty);
        p.setTopic(topic);
        p.setDescription(description);
        p.setStarterCode(starterCode);
        p.setMethodSignature(methodSig);
        p.setHints(hints);
        p.setApproach(approach);
        return problemRepository.save(p);
    }

    private void tc(Problem p, String inputs, String expected, boolean hidden, String explanation) {
        if (p == null) return;
        TestCase t = new TestCase();
        t.setProblem(p);
        t.setInputs(inputs);
        t.setExpectedOutput(expected);
        t.setHidden(hidden);
        t.setExplanation(explanation);
        testCaseRepository.save(t);
    }

    private void seedTwoSum() {
        Problem p = newProblem("lc-hm-6", "Two Sum", "Easy", "Arrays",
            "Indices of two numbers adding up to target.",
            "class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        \n    }\n}",
            "{\"name\":\"twoSum\",\"returnType\":\"int[]\",\"params\":[{\"type\":\"int[]\",\"name\":\"nums\"},{\"type\":\"int\",\"name\":\"target\"}],\"isStatic\":false}",
            "[\"Use a HashMap.\"]",
            "O(N) with HashMap.");
        if (p != null) {
            p.setKey("lc-hm-6,arr-19,two-sum");
            problemRepository.save(p);
        }
        tc(p, "{\"nums\":\"[2,7,11,15]\",\"target\":\"9\"}", "[0, 1]", false, null);
    }

    private void seedContainsDuplicate() {
        Problem p = newProblem("contains-duplicate", "Contains Duplicate", "Easy", "Arrays", "Check duplicates.", "class Solution {}", "{}", "[]", "");
        tc(p, "{\"nums\":\"[1,2,3,1]\"}", "true", false, null);
    }
    private void seedValidAnagram() {
        Problem p = newProblem("valid-anagram", "Valid Anagram", "Easy", "Strings", "Check anagram.", "class Solution {}", "{}", "[]", "");
        tc(p, "{\"s\":\"anagram\",\"t\":\"nagaram\"}", "true", false, null);
    }
    private void seedValidPalindrome() {
        Problem p = newProblem("valid-palindrome", "Valid Palindrome", "Easy", "Strings", "Check palindrome.", "class Solution {}", "{}", "[]", "");
        tc(p, "{\"s\":\"racecar\"}", "true", false, null);
    }
    private void seedMaximumConsecutiveOnes() {
        Problem p = newProblem("maximum-consecutive-ones", "Max Consecutive Ones", "Easy", "Arrays", "Count ones.", "class Solution {}", "{}", "[]", "");
        tc(p, "{\"nums\":\"[1,1,0,1,1,1]\"}", "3", false, null);
    }
    private void seedFindMissingNumber() {
        Problem p = newProblem("arr-39", "Find Missing Number", "Easy", "Arrays", "Missing num.", "class Solution {}", "{}", "[]", "");
        tc(p, "{\"nums\":\"[3,0,1]\"}", "2", false, null);
    }
    private void seedMoveZeroes() {
        Problem p = newProblem("arr-36", "Move Zeroes", "Easy", "Arrays", "Move 0s.", "class Solution {}", "{}", "[]", "");
        tc(p, "{\"nums\":\"[0,1,0,3,12]\"}", "void", false, null);
    }
    private void seedIntersectionOfTwoSortedArrays() {
        Problem p = newProblem("arr-38", "Intersection", "Easy", "Arrays", "Intersection.", "class Solution {}", "{}", "[]", "");
        tc(p, "{\"nums1\":\"[2]\",\"nums2\":\"[2]\"}", "[2]", false, null);
    }
    private void seedUnionOfTwoSortedArrays() {
        Problem p = newProblem("arr-37", "Union", "Easy", "Arrays", "Union.", "class Solution {}", "{}", "[]", "");
        tc(p, "{\"nums1\":\"[1]\",\"nums2\":\"[2]\"}", "[1, 2]", false, null);
    }
    private void seedLargestElementInArray() {
        Problem p = newProblem("arr-31", "Largest", "Easy", "Arrays", "Max.", "class Solution {}", "{}", "[]", "");
        tc(p, "{\"nums\":\"[1,2,3]\"}", "3", false, null);
    }
    private void seedSecondLargestElement() {
        Problem p = newProblem("arr-32", "Second Largest", "Easy", "Arrays", "2nd Max.", "class Solution {}", "{}", "[]", "");
        tc(p, "{\"nums\":\"[1,2,3]\"}", "2", false, null);
    }
    private void seedCheckIfArrayIsSorted() {
        Problem p = newProblem("arr-33", "Is Sorted", "Easy", "Arrays", "Check sorted.", "class Solution {}", "{}", "[]", "");
        tc(p, "{\"nums\":\"[1,2,3]\"}", "true", false, null);
    }
    private void seedRemoveDuplicatesFromSortedArray() {
        Problem p = newProblem("arr-27", "Remove Duplicates", "Easy", "Arrays", "Unique count.", "class Solution {}", "{}", "[]", "");
        tc(p, "{\"nums\":\"[1,1,2]\"}", "2", false, null);
    }
    private void seedStockBuyAndSell() {
        Problem p = newProblem("arr-6", "Stock Profit", "Easy", "Arrays", "Max profit.", "class Solution {}", "{}", "[]", "");
        tc(p, "{\"prices\":\"[7,1,5]\"}", "4", false, null);
    }
    private void seedKadanesAlgorithm() {
        Problem p = newProblem("arr-4", "Kadane", "Medium", "Arrays", "Max sub sum.", "class Solution {}", "{}", "[]", "");
        tc(p, "{\"nums\":\"[-1,2,3]\"}", "5", false, null);
    }
    private void seedMajorityElementN2() {
        Problem p = newProblem("arr-15", "Majority Element", "Easy", "Arrays", ">n/2", "class Solution {}", "{}", "[]", "");
        tc(p, "{\"nums\":\"[2,2,1]\"}", "2", false, null);
    }

    @Transactional
    public Problem generateAndSaveProblem(String key, String title) throws Exception {
        System.out.println("🤖 Generating: " + title);
        String json = groqService.generateProblemData(key, title);
        json = cleanAiJson(json);
        JsonNode root = objectMapper.readTree(json);

        Problem p = new Problem();
        p.setKey(key);
        p.setTitle(root.path("title").asText(title));
        p.setDescription(root.path("description").asText());
        p.setDifficulty(root.path("difficulty").asText("Medium"));
        p.setTopic(root.path("topic").asText("General"));
        p.setStarterCode(root.path("starterCode").asText());
        p.setMethodSignature(objectMapper.writeValueAsString(root.path("methodSignature")));
        p.setHints(objectMapper.writeValueAsString(root.path("hints")));
        p.setApproach(root.path("approach").asText());
        
        p = problemRepository.save(p);

        JsonNode cases = root.path("testCases");
        if (cases != null && cases.isArray() && cases.size() > 0) {
            for (JsonNode node : cases) {
                TestCase t = new TestCase();
                t.setProblem(p);
                t.setInputs(objectMapper.writeValueAsString(node.path("inputs")));
                t.setExpectedOutput(node.path("expectedOutput").asText());
                t.setHidden(node.path("isHidden").asBoolean(true));
                t.setExplanation(node.path("explanation").asText(null));
                testCaseRepository.save(t);
            }
        } else {
            System.out.println("⚠️ Warning: No test cases generated for " + title);
            throw new RuntimeException("AI failed to generate test cases for: " + title);
        }
        return p;
    }

    private String cleanAiJson(String json) {
        if (json == null) return null;
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return json.substring(start, end + 1);
        }
        return json.trim();
    }
}
