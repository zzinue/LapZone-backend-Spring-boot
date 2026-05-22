package com.lapzone.api.auth;

import com.lapzone.api.user.AppUser;

import java.util.UUID;

public record AuthResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String role,
        String token,
        String message
) {

    public static AuthResponse fromUser(AppUser user, String token, String message) {
        return new AuthResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole().getName(),
                token,
                message
        );
    }
}