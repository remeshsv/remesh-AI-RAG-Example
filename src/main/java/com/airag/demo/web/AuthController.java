package com.airag.demo.web;

import com.airag.demo.model.User;
import com.airag.demo.repository.UserRepository;
import com.airag.demo.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepo, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<Map<String, String>>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        System.out.println(username);
        if (username == null || password == null) {
            return Mono.<ResponseEntity<Map<String, String>>>just(ResponseEntity.<Map<String, String>>badRequest().build());
        }
        return userRepo.findByUsername(username.trim())
                .flatMap(user -> {
                    if (!passwordEncoder.matches(password, user.getPassword())) {
                        return Mono.<ResponseEntity<Map<String, String>>>just(ResponseEntity.<Map<String, String>>status(HttpStatus.UNAUTHORIZED).build());
                    }
                    System.out.println(user.getRoles());
                    String token = jwtUtil.generateToken(user.getUsername(), user.getRoles());
                    System.out.println(token);
                    return Mono.just(ResponseEntity.ok(Map.of("token", token, "role", user.getRoles())));
                })
                .switchIfEmpty(Mono.<ResponseEntity<Map<String, String>>>just(
                        ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
                ));
    }
}
