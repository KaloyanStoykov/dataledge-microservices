package org.dataledge.identityservice.service;

import org.dataledge.identityservice.entity.UserCredential;
import org.dataledge.identityservice.repository.UserCredentialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserCredentialRepository repository;

    public String saveUser(UserCredential userCredential) {
        // ToDo: Type checking and validation
        repository.save(userCredential);
        return "user added to the system"
    }
}
