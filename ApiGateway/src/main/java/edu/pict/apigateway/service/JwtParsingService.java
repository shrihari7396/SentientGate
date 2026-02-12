package edu.pict.apigateway.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class JwtParsingService {

    private final JwtDecoder jwtDecoder;

    public ParsedJwt parse(String token) throws JwtException {

        Jwt jwt = jwtDecoder.decode(token);

        String jti = jwt.getId();          // jti claim
        String subject = jwt.getSubject(); // sub claim
        Instant expiresAt = jwt.getExpiresAt();

        if (jti == null || expiresAt == null) {
            throw new JwtException("Missing required JWT claims");
        }

        return new ParsedJwt(
                jti,
                subject,
                expiresAt.getEpochSecond()
        );
    }

    public record ParsedJwt(
            String jti,
            String subject,
            long expEpochSeconds
    ) {}
}