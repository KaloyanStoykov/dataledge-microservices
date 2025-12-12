package org.dataledge.datasourceservice.config.exceptions;

/**
 * Exception is thrown in the AzureBlobRequestManager for sanitizing user ID's from request headers
 * Checks strictly numeric userIds coming from request header in controller layer.
 * in order to prevent malicious entries for folder structures.
 */
public class InvalidUserException extends RuntimeException {

    /**
     * Constructs a new InvalidUserException with the specified detail message.
     * * @param message the detail message.
     */
    public InvalidUserException(String message) {
        super(message);
    }
}
