package org.example.tenantmvc.Exception;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor

public class DuplicateTenantException extends RuntimeException {
    public DuplicateTenantException(String message){
        super(message);
    }
}
