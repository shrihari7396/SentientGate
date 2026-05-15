package edu.pict.apigateway.service.impl;

import edu.pict.apigateway.service.SentinelSecurityService;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class SentinelSecurityServiceImpl implements SentinelSecurityService {

    private final SecretKeySpec hmacSecretKey;
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Override
    public String generateSignedId() {
        String uuid = UUID.randomUUID().toString();
        String signature = calculateHmac(uuid);
        return uuid + "." + signature;
    }

    @Override
    public String verifyAndExtractId(String fullToken) {
        if (fullToken == null || !fullToken.contains(".")) return null;

        String[] parts = fullToken.split("\\.");
        if (parts.length != 2) return null;

        String uuid = parts[0];
        String receivedSignature = parts[1];
        String expectedSignature = calculateHmac(uuid);

        if (MessageDigest.isEqual(receivedSignature.getBytes(), expectedSignature.getBytes())) {
            return uuid;
        }
        log.warn("⚠️ Security Alert: Tampered or invalid cookie detected!");
        return null;
    }

    private String calculateHmac(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(hmacSecretKey);
            byte[] hmacBytes = mac.doFinal(data.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Cryptographic failure: {}", e.getMessage());
            throw new RuntimeException("Internal Security Error");
        }
    }
}
