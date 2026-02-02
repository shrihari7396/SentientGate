package edu.pict.apigateway.filters.route;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

@Component
public class CustomHeaderFilter extends AbstractGatewayFilterFactory<CustomHeaderFilter.Config> {


    @Override
    public GatewayFilter apply(Config config) {

        return null;
    }

    public static class Config {

    }
}
