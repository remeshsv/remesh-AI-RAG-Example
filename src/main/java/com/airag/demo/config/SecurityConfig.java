package com.airag.demo.config;

import com.airag.demo.security.JwtAuthenticationWebFilter;
import com.airag.demo.security.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.WebFilter;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .authorizeExchange(ex -> ex
                        .pathMatchers("/actuator/**", "/ping", "/api/auth/login", "/api/chat/**", "/api/rag/**").permitAll()
                        .anyExchange().authenticated()
                )
                .build();
    }

    // register the JWT webfilter (it is a global WebFilter bean)
    @Bean
    public WebFilter jwtAuthenticationWebFilter(JwtUtil jwtUtil) {
        return new JwtAuthenticationWebFilter(jwtUtil);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
