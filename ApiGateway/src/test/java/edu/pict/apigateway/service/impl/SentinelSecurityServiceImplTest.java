package edu.pict.apigateway.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SentinelSecurityServiceImplTest {

    private SentinelSecurityServiceImpl securityService;
    private final String secret = "this-is-a-very-secure-secret-key-32-chars!!";

    @BeforeEach
    void setUp() {
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        securityService = new SentinelSecurityServiceImpl(secretKeySpec);
    }

    @Test
    void testGenerateSignedId() {
        String signedId = securityService.generateSignedId();
        assertNotNull(signedId);
        assertTrue(signedId.contains("."));

        String[] parts = signedId.split("\\.");
        assertEquals(2, parts.length);

        // Verify it can be extracted back
        String extractedId = securityService.verifyAndExtractId(signedId);
        assertEquals(parts[0], extractedId);
        assertDoesNotThrow(() -> UUID.fromString(extractedId));
    }

    @Test
    void testVerifyAndExtractId_Valid() {
        String uuid = UUID.randomUUID().toString();
        // Since we can't easily calculate HMAC here without duplicating logic,
        // we use the service to generate one first or just use the extracted one.
        String signedId = securityService.generateSignedId();
        String extractedId = securityService.verifyAndExtractId(signedId);
        assertNotNull(extractedId);
        assertEquals(signedId.split("\\.")[0], extractedId);
    }

    @Test
    void testVerifyAndExtractId_InvalidSignature() {
        String signedId = securityService.generateSignedId();
        String tamperedId = signedId.substring(0, signedId.length() - 5) + "abcde";
        String extractedId = securityService.verifyAndExtractId(tamperedId);
        assertNull(extractedId);
    }

    @Test
    void testVerifyAndExtractId_Malformed() {
        assertNull(securityService.verifyAndExtractId(null));
        assertNull(securityService.verifyAndExtractId("no-dot-here"));
        assertNull(securityService.verifyAndExtractId("too.many.dots.here"));
    }

    @Test
    void testVerifyAndExtractId_TamperedUuid() {
        String signedId = securityService.generateSignedId();
        String[] parts = signedId.split("\\.");
        String tamperedUuid = UUID.randomUUID().toString();
        String tamperedId = tamperedUuid + "." + parts[1];

        String extractedId = securityService.verifyAndExtractId(tamperedId);
        assertNull(extractedId);
    }
}
