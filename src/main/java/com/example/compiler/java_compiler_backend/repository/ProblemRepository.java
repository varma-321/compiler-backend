package com.example.compiler.java_compiler_backend.repository;

import com.example.compiler.java_compiler_backend.model.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long> {
    Optional<Problem> findByKey(String key);
}
