package com.fileupload.auth.dto;

import com.fileupload.auth.security.AuthenticatedUser;

public record AuthUserResponse(
    Long id,
    String username,
    String role
) {

    public static AuthUserResponse from(
        AuthenticatedUser user
    ) {
        return new AuthUserResponse(
            user.getId(),
            user.getUsername(),
            user.getRole().name()
        );
    }
}