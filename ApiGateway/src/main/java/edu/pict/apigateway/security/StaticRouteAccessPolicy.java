package edu.pict.apigateway.security;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

@Component
@RequiredArgsConstructor
public class StaticRouteAccessPolicy implements RouteAccessPolicy {

    private static final List<String> PUBLIC_PATTERNS =
            List.of(
                    "/api/auth/**",
                    "/actuator/health",
                    "/actuator/info",
                    "/favicon.ico");

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public boolean isPublicPath(String path) {
        return PUBLIC_PATTERNS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
}

