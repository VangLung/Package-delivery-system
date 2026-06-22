package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;

class JwtServiceTest {

    private final JwtService jwtService =
        new JwtService("test-secret-please-use-a-long-random-value-here");

    @Test
    void generatesAndParsesToken() {
        String token = jwtService.generate("alice", "admin");

        Claims claims = jwtService.parse(token);

        assertEquals("alice", claims.getSubject());
        assertEquals("admin", claims.get("role", String.class));
    }

    @Test
    void rejectsInvalidToken() {
        assertThrows(Exception.class, () -> jwtService.parse("garbage.token.value"));
    }
}
