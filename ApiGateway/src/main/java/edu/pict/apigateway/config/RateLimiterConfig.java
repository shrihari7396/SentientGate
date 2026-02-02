package edu.pict.apigateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
@RequiredArgsConstructor
public class RateLimiterConfig {
    @Bean
    public DefaultRedisScript<Long> rateLimitScript() {

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText("""
            if redis.call('INCR', KEYS[1]) == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return redis.call('GET', KEYS[1])
        """);
        script.setResultType(Long.class);
        return script;
    }
}
