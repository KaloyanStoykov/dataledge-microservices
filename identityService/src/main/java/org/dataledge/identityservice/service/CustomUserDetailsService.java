package org.dataledge.identityservice.service;

import lombok.extern.slf4j.Slf4j;
import org.dataledge.identityservice.config.CustomUserDetails;
import org.dataledge.identityservice.entity.UserCredential;
import org.dataledge.identityservice.repository.UserCredentialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    /***
     *
     * @param query can be either a String id for int parsing or a string of email
     * @return UserDetails that points to existing user credential
     * @throws UsernameNotFoundException when no user is found by either types
     */
    @Override
    public UserDetails loadUserByUsername(String query) throws UsernameNotFoundException {
        Optional<UserCredential> credential = userCredentialRepository.findByEmail(query);
        log.info("UserDetails from database user credential entity");
        return credential.map(CustomUserDetails::new).orElseThrow(() -> new UsernameNotFoundException("User not found with name: " + query));
    }


}
