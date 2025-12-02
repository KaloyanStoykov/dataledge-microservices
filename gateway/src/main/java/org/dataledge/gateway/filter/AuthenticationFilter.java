package org.dataledge.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.dataledge.gateway.config.exceptions.UnauthorizedException;
import org.dataledge.gateway.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Autowired
    private RouteValidator validator;

    @Autowired
    private JwtUtil jwtUtil;

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // 1. Check if the route requires security
            if (validator.isSecured.test(request)) {

                String token = null;

                // 2. STRATEGY A: Try to get Token from Cookies (Priority for Frontend)
                if (request.getCookies().containsKey("accessToken")) {
                    HttpCookie cookie = request.getCookies().getFirst("accessToken");
                    if (cookie != null) {
                        token = cookie.getValue();
                    }
                }

                // 4. If we still don't have a token, throw error
                if (token == null) {
                    throw new UnauthorizedException("Missing authorization cookie or header");
                }

                // 5. Validate the found token
                try {
                    jwtUtil.validateToken(token);

                    String userId = jwtUtil.extractUserIdClaim(token);


                    request = exchange.getRequest()
                            .mutate()
                            .header("X-User-ID", userId)
                            .build();
                } catch (Exception e) {
                    //log.error("Invalid Token: " + e.getMessage());
                    throw new UnauthorizedException("Unauthorized access");
                }

                return chain.filter(exchange.mutate().request(request).build());
            }

            // If route is NOT secured, just pass the original request
            return chain.filter(exchange);
        });
    }

    public static class Config {
    }
}