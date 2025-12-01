package org.dataledge.identityservice.service;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dataledge.identityservice.config.CustomUserDetails;
import org.dataledge.identityservice.config.exceptions.ExistingEmailException;
import org.dataledge.identityservice.dto.auth.AuthRequest;
import org.dataledge.identityservice.dto.auth.AuthResponse;
import org.dataledge.identityservice.dto.auth.SignUpResponse;
import org.dataledge.identityservice.dto.auth.User;
import org.dataledge.identityservice.entity.UserCredential;
import org.dataledge.identityservice.repository.UserCredentialRepository;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@AllArgsConstructor
@Service
@Slf4j
public class AuthService {

    private UserCredentialRepository repository;

    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;

    private JwtUtil jwtService;

    private final RabbitMQProducer rabbitMQProducer;

    public SignUpResponse saveUser(UserCredential userCredential) {
        Optional<UserCredential> existingCredential = repository.findByEmail(userCredential.getEmail());
        if (existingCredential.isPresent()) {
            log.info("User already exists with email {}", userCredential.getEmail());
            throw new ExistingEmailException("Email already exists!");
        }
        userCredential.setPassword(passwordEncoder.encode(userCredential.getPassword()));
        repository.save(userCredential);
        log.info("User saved with email {}", userCredential.getEmail());
        return new SignUpResponse("Your account has been created!");
    }

    public AuthResponse authenticate(AuthRequest req) {
        AuthResponse response = new AuthResponse();
        try {
            Authentication authenticate = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
            );

            if(authenticate.isAuthenticated()) {
                CustomUserDetails principal = (CustomUserDetails)authenticate.getPrincipal();

                Optional<UserCredential> credential = repository.findByEmail(principal.getUsername());

                if (credential.isPresent()) {
                    UserCredential userCredential = credential.get();

                    response.setUser(new User(userCredential.getId(), userCredential.getEmail(), userCredential.getName()));

                    String email = userCredential.getEmail();
                    String userId = String.valueOf(userCredential.getId());

                    log.info("Setting response jwt token...");
                    response.setJwtToken(jwtService.generateToken(email, userId));

                    return response;
                } else {
                    // Should not happen if authentication passed, but good for safety
                    throw new BadCredentialsException("User credential not found after successful authentication.");
                }
            }
        }
        catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return response;
    }

    @Transactional
    public void deletePersonalAccount(Integer requestUID, Integer userId) {
        if(!Objects.equals(requestUID, userId)) {
            log.error("Invalid delete request");
            throw new BadCredentialsException("Invalid request UID");
        }

        UserCredential user = repository.findById(userId)
                .orElse(null);

        if (user == null) {
            log.error("Invalid user found with requested id: {}", userId );
            throw new NotFoundException("User with ID " + userId + " not found.");
        }

        repository.delete(user);
        log.info("Deleted user with ID {}", userId);

        log.info("Sending message to RabbitMQ for user {}", userId);
        rabbitMQProducer.sendUserDeletedEvent(userId);
        log.info("Message sent to RabbitMQ for user {}", userId);
    }



    // 2. Validate structure/expiration only (Used by /validate endpoint)
    public void validateToken(String token) {
        jwtService.validateToken(token);
    }

    public User checkAuth(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {

            // 1. Get the email from the session/token
            String email = authentication.getName();

            // 2. Fetch the full user details from the DB
            UserCredential user = repository.findByEmail(email).orElseThrow(() -> new BadCredentialsException("User not found!"));


            // 3. Return the DTO with ID, Email and Name
            return new User(user.getId(), user.getEmail(), user.getName());
        }

        throw new AuthenticationCredentialsNotFoundException("User is currently not authenticated");
    }
}