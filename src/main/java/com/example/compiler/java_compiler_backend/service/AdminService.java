package com.example.compiler.java_compiler_backend.service;

import com.example.compiler.java_compiler_backend.model.Admin;
import com.example.compiler.java_compiler_backend.repository.AdminRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class AdminService {

    private final AdminRepository adminRepository;
    private final JavaMailSender mailSender;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public AdminService(AdminRepository adminRepository, JavaMailSender mailSender) {
        this.adminRepository = adminRepository;
        this.mailSender = mailSender;
    }

    public Admin signup(String name, String email, String password) throws Exception {
        if (adminRepository.findByEmail(email).isPresent()) {
            throw new Exception("Email already registered");
        }

        Admin admin = new Admin();
        admin.setName(name);
        admin.setEmail(email);
        admin.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        admin.setStatus("PENDING_APPROVAL");
        
        String token = UUID.randomUUID().toString();
        admin.setApprovalToken(token);

        Admin savedAdmin = adminRepository.save(admin);

        // Send email to the hardcoded super-admin email
        sendApprovalEmail("yashwanthvarma.simats@gmail.com", savedAdmin.getEmail(), token);

        return savedAdmin;
    }

    private void sendApprovalEmail(String to, String adminEmail, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("New Admin Approval Required: " + adminEmail);
            String approvalUrl = "https://compiler-backend-perc.onrender.com/api/admin/approve?token=" + token;
            message.setText("A new user requested admin access: " + adminEmail + "\n\n" +
                    "Click the following link to approve:\n" + approvalUrl);
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Could not send email. Approval token: " + token);
        }
    }

    public boolean approveAdmin(String token) {
        Optional<Admin> optionalAdmin = adminRepository.findByApprovalToken(token);
        if (optionalAdmin.isPresent()) {
            Admin admin = optionalAdmin.get();
            admin.setStatus("APPROVED");
            admin.setApprovalToken(null);
            adminRepository.save(admin);
            return true;
        }
        return false;
    }

    public String login(String email, String password) throws Exception {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new Exception("Invalid email or password"));

        if (!BCrypt.checkpw(password, admin.getPassword())) {
            throw new Exception("Invalid email or password");
        }

        if (!"APPROVED".equals(admin.getStatus())) {
            throw new Exception("Account is pending approval");
        }

        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return Jwts.builder()
                .setSubject(admin.getEmail())
                .claim("role", "admin")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 1 day
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }
}
