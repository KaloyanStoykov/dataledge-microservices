package org.dataledge.identityservice.service;

import org.dataledge.identityservice.config.CustomUserDetails;
import org.dataledge.identityservice.config.exceptions.ExistingEmailException;
import org.dataledge.identityservice.dto.auth.AuthRequest;
import org.dataledge.identityservice.dto.auth.AuthResponse;
import org.dataledge.identityservice.dto.auth.SignUpResponse;
import org.dataledge.identityservice.dto.auth.User;
import org.dataledge.identityservice.entity.UserCredential;
import org.dataledge.identityservice.repository.UserCredentialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserCredentialRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserCredentialRepository userRepository;

    @Autowired
    private JWTService jwtService;

    public SignUpResponse saveUser(UserCredential userCredential) {
        Optional<UserCredential> existingCredential = userRepository.findByEmail(userCredential.getEmail());
        if (existingCredential.isPresent()) {
            throw new ExistingEmailException("Email already exists!");
        }
        userCredential.setPassword(passwordEncoder.encode(userCredential.getPassword()));
        repository.save(userCredential);
        return new SignUpResponse("Your account has been created!");
    }

    public AuthResponse authenticate(AuthRequest req) {
        AuthResponse response = new AuthResponse();
        try {
            Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
            if(authenticate.isAuthenticated()) {
                CustomUserDetails principal = (CustomUserDetails)authenticate.getPrincipal();
                Optional<UserCredential> credential = userRepository.findByEmail(principal.getUsername());
                // Check for user stored in db and map to dto.
                credential.ifPresent(userCredential -> response.setUser(new User(userCredential.getId(), userCredential.getEmail(), userCredential.getName())));
                response.setJwtToken(generateToken(req.getEmail()));
                return response;
            }
        }
        catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return response;
    }

    public String generateToken(String email) {
        return jwtService.generateToken(email);
    }

    public void validateToken(String token) {
        jwtService.validateToken(token);
    }
}
