package org.example.auth.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.auth.Entity.Role;
import java.util.Set;

public class AuthDto {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Register{

        @NotBlank(message = "First name is required")
        private String firstName;

        @NotBlank(message = "Last name is required")
        private String lastName;

        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;

        @NotBlank(message = "Tenant ID is required")
        private String tenantId;

        private Set<Role> roles;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest{

        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthResponse{

        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private long expiresIn;
        private String userId;
        private String email;
        private String tenantId;
        private Set<Role>roles;

    }
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshTokenRequest{

        @NotBlank(message = "Refresh Token is required")
        private String refreshToken;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenValidationResponse {
        private boolean valid;
        private String userId;
        private String email;
        private String tenantId;
        private Set<Role> roles;
        private String message; // Error message if token is invalid
    }

}
