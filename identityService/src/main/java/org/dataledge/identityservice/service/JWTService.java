package org.dataledge.identityservice.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.dataledge.identityservice.entity.UserCredential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JWTService {
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

    // Refactored method for clarity, now uses the correct variable names
    String generateJwtToken(Authentication authentication) {
        // Assuming your UserDetailsImpl class is correct
        UserCredential userPrincipal = (UserCredential) authentication.getPrincipal();

        return Jwts.builder()
                .subject((userPrincipal.getEmail()))
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSignKey()) // Uses getSignKey() which uses the injected 'secret'
                .compact();
    }

    // --- Key Derivation ---

    private Key getSignKey() {
        // Uses the injected instance field 'secret'
        byte[] keyBytes = Decoders.BASE64.decode(this.secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
