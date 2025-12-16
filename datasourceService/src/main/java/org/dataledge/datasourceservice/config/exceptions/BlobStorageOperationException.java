package org.dataledge.datasourceservice.config.exceptions;

public class BlobStorageOperationException extends RuntimeException {
    /**
     * Constructs a new BlobStorageOperationException with the specified detail message and cause.
     * This is used when wrapping the original Azure SDK exception.
     * @param message the detail message (which is saved for later retrieval by the getMessage() method).
     * @param cause the cause (which is saved for later retrieval by the getCause() method).
     * (A null value is permitted, and indicates that the cause is unknown or non-existent.)
     */
    public BlobStorageOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new BlobStorageOperationException with the specified detail message.
     * @param message the detail message.
     */
    public BlobStorageOperationException(String message) {
        super(message);
    }
}
