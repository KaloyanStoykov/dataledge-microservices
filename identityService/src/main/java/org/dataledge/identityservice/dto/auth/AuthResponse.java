package org.dataledge.identityservice.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dataledge.identityservice.entity.UserCredential;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private User user;
    private String jwtToken;
}
