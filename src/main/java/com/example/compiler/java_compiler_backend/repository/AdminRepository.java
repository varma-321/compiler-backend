package com.example.compiler.java_compiler_backend.repository;

import com.example.compiler.java_compiler_backend.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByEmail(String email);
    Optional<Admin> findByApprovalToken(String approvalToken);
}
