package edu.pict.apigateway.service;

import io.jsonwebtoken.Claims;

import java.util.List;

public interface JwtService {

    void init();
    Claims validateAndExtractClaims(String token);
    String extractJti(String token);
    String extractUserId(String token);
    List<String> extractRoles(String token);
}
