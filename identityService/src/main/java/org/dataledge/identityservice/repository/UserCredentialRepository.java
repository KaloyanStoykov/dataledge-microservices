package org.dataledge.identityservice.repository;

import org.dataledge.identityservice.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCredentialRepository extends JpaRepository<UserCredential,  Integer> {
}
