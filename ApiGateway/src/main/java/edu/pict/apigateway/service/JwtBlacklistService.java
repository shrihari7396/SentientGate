package edu.pict.apigateway.service;

import reactor.core.publisher.Mono;

public interface JwtBlacklistService {
    Mono<Boolean> isBlocked(String jti);
}
