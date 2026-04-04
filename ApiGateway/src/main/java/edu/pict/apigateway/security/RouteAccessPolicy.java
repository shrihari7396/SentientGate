package edu.pict.apigateway.security;

public interface RouteAccessPolicy {
    boolean isPublicPath(String path);

    default boolean requiresAuthentication(String path) {
        return !isPublicPath(path);
    }
}

