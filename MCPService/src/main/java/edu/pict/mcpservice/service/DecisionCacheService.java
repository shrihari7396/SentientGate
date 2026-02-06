package edu.pict.mcpservice.service;

import edu.pict.mcpservice.model.Decision;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DecisionCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void cacheByIp(String ip, Decision decision) {
        redisTemplate.opsForValue()
                .set(keyForIp(ip), decision, decision.getTtlSeconds(), TimeUnit.SECONDS);
    }

    private String keyForIp(String ip) {
        return "decision:ip:" + ip;
    }
}

