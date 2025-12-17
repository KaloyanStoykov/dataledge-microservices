package org.dataledge.identityservice.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.dataledge.identityservice.dto.UserDeletedResponse;
import org.dataledge.identityservice.dto.auth.AuthRequest;
import org.dataledge.identityservice.dto.auth.SignUpResponse;
import org.dataledge.identityservice.dto.auth.User;
import org.dataledge.identityservice.entity.UserCredential;
import org.dataledge.identityservice.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.dataledge.common.DataLedgeUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @Value("${JWT_EXPIRATION_MS}")
    private int jwtExpirationMs;

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public @ResponseBody SignUpResponse addNewUser(@RequestBody UserCredential user){
        return authService.saveUser(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logoutUser(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("accessToken", "") // Empty value
                .httpOnly(true)
                .secure(false) // strict HTTPS check (keep consistent with login)
                .path("/")     // Must match the login path exactly
                .maxAge(0)     // 0 seconds = expire immediately
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok("Logged out successfully");
    }

    @PostMapping("/authenticate")
    public ResponseEntity<User> getToken(@RequestBody AuthRequest req, HttpServletResponse response) {
        var result = authService.authenticate(req);

        ResponseCookie cookie = ResponseCookie.from("accessToken", result.getJwtToken())
                .httpOnly(true)
                .secure(false) // For HTTp
                .path("/")
                .maxAge(jwtExpirationMs)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(result.getUser());
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<UserDeletedResponse> deleteUser(@PathVariable int id,
                                                          HttpServletResponse response,
                                                          @RequestHeader(DataLedgeUtil.USER_ID_HEADER)  String userId) {


        authService.deletePersonalAccount(id, Integer.valueOf(userId));
        ResponseCookie cookie = ResponseCookie.from("accessToken", "") // Празно
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());


        return ResponseEntity.noContent().build();
    }

    @GetMapping("/validate")
    public String validateToken(@RequestParam("token") String token){
        authService.validateToken(token);
        return "Token is valid";
    }

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser() {
        return ResponseEntity.ok(authService.checkAuth());
    }
}
