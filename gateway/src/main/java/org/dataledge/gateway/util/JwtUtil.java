package org.dataledge.gateway.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
    @Value("${JWT_SECRET}")
    private String secret;

    @Value("${JWT_EXPIRATION_MS}")
    private int jwtExpirationMs;

    public void validateToken(final String token) {
        Jwts.parser()
                .verifyWith((SecretKey) getSignKey())
                .build()
                .parseSignedClaims(token);
    }


    public String generateToken(String userName) {
        Map<String, Object> claims = new HashMap<>();
        // Set up the token with subject, issue date, expiration, and signing key
        return Jwts.builder()
                .claims(claims)
                .subject(userName)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSignKey())
                .compact();
    }


    // --- Key Derivation ---

    private Key getSignKey() {
        // Uses the injected instance field 'secret'
        byte[] keyBytes = Decoders.BASE64.decode(this.secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}