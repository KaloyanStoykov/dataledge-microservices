package org.dataledge.identityservice;

import org.dataledge.identityservice.entity.UserCredential;
import org.dataledge.identityservice.repository.UserCredentialRepository;
import org.dataledge.identityservice.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserCredentialRepository userCredentialRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void loadUserByUsername_Success() {
        // Arrange
        String email = "test@example.com";
        UserCredential mockCredential = new UserCredential();
        mockCredential.setEmail(email);
        mockCredential.setPassword("encodedPassword");
        // Add roles/authorities if your CustomUserDetails requires them

        when(userCredentialRepository.findByEmail(email)).thenReturn(Optional.of(mockCredential));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        // Assert
        assertNotNull(userDetails);
        assertEquals(email, userDetails.getUsername());
        verify(userCredentialRepository, times(1)).findByEmail(email);
    }

    @Test
    void loadUserByUsername_UserNotFound_ThrowsException() {
        // Arrange
        String email = "nonexistent@example.com";
        when(userCredentialRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername(email);
        });

        verify(userCredentialRepository, times(1)).findByEmail(email);
    }
}
