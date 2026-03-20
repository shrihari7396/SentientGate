package edu.pict.apigateway.service.impl;

import edu.pict.apigateway.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtServiceImpl implements JwtService {

    @Value("${jwt.secret-key}")
    private String secretKey;

    private Key signingKey;

    @Override
    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    @Override
    public Claims validateAndExtractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    @Override
    public String extractUserId(String token) {
        return validateAndExtractClaims(token).getSubject();
    }

    @Override
    public List<String> extractRoles(String token) {
        return validateAndExtractClaims(token).get("roles", List.class);
    }

    @Override
    public String extractJti(String token) {
        return validateAndExtractClaims(token).getId();
    }
}
