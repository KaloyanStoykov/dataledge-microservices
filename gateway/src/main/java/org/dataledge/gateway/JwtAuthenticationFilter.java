package org.dataledge.gateway;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dataledge.gateway.filter.AuthenticationFilter;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

//@Component
//@Slf4j
//public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {
//
//    private final WebClient webClient;
//
//    public JwtAuthenticationFilter(ReactorLoadBalancerExchangeFilterFunction exchangeFilterFunction) {
//        super(Config.class);
//    }
//
//    /**
//     * @param config
//     * @return
//     */
//    @Override
//    public GatewayFilter apply(NameConfig config) {
//
//    }
//
//
//    @Override
//    public GatewayFilter apply(AuthenticationFilter.Config config) {
//        return null;
//    }
//
//    @Data
//    public static class Config {
//        private boolean enabled;
//    }
//}
