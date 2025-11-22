package org.dataledge.identityservice.controller;

import org.apache.tomcat.websocket.AuthenticationException;
import org.dataledge.identityservice.dto.auth.AuthRequest;
import org.dataledge.identityservice.dto.auth.AuthResponse;
import org.dataledge.identityservice.dto.auth.SignUpResponse;
import org.dataledge.identityservice.entity.UserCredential;
import org.dataledge.identityservice.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public @ResponseBody SignUpResponse addNewUser(@RequestBody UserCredential user){
        return authService.saveUser(user);
    }

    @PostMapping("/authenticate")
    public @ResponseBody AuthResponse getToken(@RequestBody AuthRequest req) {
        return authService.authenticate(req);
    }

    @GetMapping("/validate")
    public String validateToken(@RequestParam("token") String token){
        authService.validateToken(token);
        return "Token is valid";
    }
}
