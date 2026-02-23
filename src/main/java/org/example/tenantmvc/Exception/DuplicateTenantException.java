package org.example.tenantmvc.Exception;

public class DuplicateTenantException extends RuntimeException {

    public DuplicateTenantException(String message) {
        super(message);
    }

    public DuplicateTenantException(String message, Throwable cause) {
        super(message, cause);
    }
}