package org.dataledge.identityservice.config.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// The annotation expects HttpStatus or String values, not a Class object
@ResponseStatus(value = HttpStatus.CONFLICT, reason = "Email already exists in the system.")
public class ExistingEmailException extends RuntimeException {
    public ExistingEmailException(String message) {
        super(message);
    }
}