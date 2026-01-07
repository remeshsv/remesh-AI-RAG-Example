package com.airag.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JwtAuthenticationWebFilter implements WebFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationWebFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String header = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Jws<Claims> claims = jwtUtil.parseToken(token);
                String username = claims.getBody().getSubject();
                String rolesStr = claims.getBody().get("roles", String.class);
                List<SimpleGrantedAuthority> authorities = List.of();
                if (rolesStr != null && !rolesStr.isBlank()) {
                    authorities = Arrays.stream(rolesStr.split(","))
                            .map(String::trim)
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                }
                Authentication auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
                // propagate authentication via ReactiveSecurityContext
                return chain.filter(exchange).contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
            } catch (Exception ex) {
                // invalid token -> continue without auth
            }
        }
        return chain.filter(exchange);
    }
}

