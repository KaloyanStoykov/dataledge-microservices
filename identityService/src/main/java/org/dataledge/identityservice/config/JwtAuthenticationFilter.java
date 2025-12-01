package org.dataledge.identityservice.config;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.dataledge.identityservice.service.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

/***
 * @author kiko
 * Handles jwt authentication requests to the gateway
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // Utilities to extract claims and username from token
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    // Cookie header name
    private static final String COOKIE_NAME = "accessToken";

    /***
     *
     * @param request incoming request from API call
     * @param response to be modified with JWT token and claims
     * @param chain filter chain that processes the request
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String token = null;
        String email;

        // Try to get token from header
        if (request.getCookies() != null) {
            token = Arrays.stream(request.getCookies())
                    .filter(c -> COOKIE_NAME.equals(c.getName()))
                    .map(Cookie::getValue)
                    .map(String::trim)
                    .findFirst()
                    .orElse(null);
        }
        // Token found
        if (token != null) {
            try {
                log.info("Attempting to authenticate using JWT Token");
                email = jwtUtil.extractUsername(token);


                var currentAuth = SecurityContextHolder.getContext().getAuthentication();

                // Check for anonymous user token
                if (email != null && (currentAuth == null || "anonymousUser".equals(currentAuth.getName()))) {
                    log.info("User is authenticated");
                    // Add logged in userDetails
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(email);

                    if (jwtUtil.validateToken(token, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            } catch (Exception e) {
                log.error("JWT Authentication failed: {}", e.getLocalizedMessage());
            }
        }

        chain.doFilter(request, response);
    }
}