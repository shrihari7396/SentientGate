package edu.pict.apigateway.service;

public interface SentinelSecurityService {
    String generateSignedId();

    String verifyAndExtractId(String fullToken);
}
