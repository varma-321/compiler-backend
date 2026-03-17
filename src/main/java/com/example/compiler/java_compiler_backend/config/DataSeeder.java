package com.example.compiler.java_compiler_backend.config;

import com.example.compiler.java_compiler_backend.model.Admin;
import com.example.compiler.java_compiler_backend.repository.AdminRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner seedDatabase(AdminRepository adminRepository) {
        return args -> {
            String defaultEmail = "yashwanth.simats@gmail.com";
            if (adminRepository.findByEmail(defaultEmail).isEmpty()) {
                Admin admin = new Admin();
                admin.setName("SYSTEM ADMIN");
                admin.setEmail(defaultEmail);
                admin.setPassword(BCrypt.hashpw("Varma@2820", BCrypt.gensalt()));
                admin.setStatus("APPROVED");
                adminRepository.save(admin);
                System.out.println("Seeded default SYSTEM ADMIN account.");
            }
        };
    }
}
