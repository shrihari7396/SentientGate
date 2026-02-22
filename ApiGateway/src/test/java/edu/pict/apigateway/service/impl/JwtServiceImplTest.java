package edu.pict.apigateway.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceImplTest {

    private JwtServiceImpl jwtService;
    private final String secret = "this-is-a-very-secure-secret-key-32-chars!!";
    private Key signingKey;

    @BeforeEach
    void setUp() {
        jwtService = new JwtServiceImpl();
        ReflectionTestUtils.setField(jwtService, "secretKey", secret);
        jwtService.init();
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    private String generateToken(String sub, String jti, List<String> roles, Date expiration) {
        return Jwts.builder()
                .setSubject(sub)
                .setId(jti)
                .claim("roles", roles)
                .setExpiration(expiration)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void testValidateAndExtractClaims() {
        String sub = "user123";
        String jti = UUID.randomUUID().toString();
        List<String> roles = List.of("ROLE_USER");
        Date exp = new Date(System.currentTimeMillis() + 3600000);

        String token = generateToken(sub, jti, roles, exp);

        Claims claims = jwtService.validateAndExtractClaims(token);

        assertEquals(sub, claims.getSubject());
        assertEquals(jti, claims.getId());
        assertEquals(roles, claims.get("roles"));
    }

    @Test
    void testExtractUserId() {
        String sub = "test-user";
        String token = generateToken(sub, "jti", List.of(), new Date(System.currentTimeMillis() + 10000));
        assertEquals(sub, jwtService.extractUserId(token));
    }

    @Test
    void testExtractJti() {
        String jti = "test-jti";
        String token = generateToken("sub", jti, List.of(), new Date(System.currentTimeMillis() + 10000));
        assertEquals(jti, jwtService.extractJti(token));
    }

    @Test
    void testExtractRoles() {
        List<String> roles = List.of("ADMIN", "USER");
        String token = generateToken("sub", "jti", roles, new Date(System.currentTimeMillis() + 10000));
        assertEquals(roles, jwtService.extractRoles(token));
    }

    @Test
    void testInvalidToken() {
        assertThrows(Exception.class, () -> jwtService.validateAndExtractClaims("invalid.token.here"));
    }
}
