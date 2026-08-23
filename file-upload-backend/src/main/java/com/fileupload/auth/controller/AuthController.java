package com.fileupload.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fileupload.auth.dto.AuthErrorResponse;
import com.fileupload.auth.dto.AuthUserResponse;
import com.fileupload.auth.dto.LoginRequest;
import com.fileupload.auth.security.AuthenticatedUser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy
        sessionAuthenticationStrategy;

    public AuthController(
        AuthenticationManager authenticationManager,
        SecurityContextRepository securityContextRepository,
        SessionAuthenticationStrategy sessionAuthenticationStrategy
    ) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy =
            sessionAuthenticationStrategy;
    }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken csrfToken) {
        return csrfToken;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
        @Valid @RequestBody LoginRequest loginRequest,
        HttpServletRequest request,
        HttpServletResponse response
    ) {

        try {
            Authentication authenticationRequest =
                UsernamePasswordAuthenticationToken
                    .unauthenticated(
                        loginRequest.username(),
                        loginRequest.password()
                    );

            Authentication authentication =
                authenticationManager.authenticate(
                    authenticationRequest
                );

            sessionAuthenticationStrategy.onAuthentication(
                authentication,
                request,
                response
            );

            SecurityContext context =
                SecurityContextHolder.createEmptyContext();

            context.setAuthentication(authentication);

            SecurityContextHolder.setContext(context);

            securityContextRepository.saveContext(
                context,
                request,
                response
            );

            AuthenticatedUser user =
                (AuthenticatedUser)
                    authentication.getPrincipal();

            return ResponseEntity.ok(
                AuthUserResponse.from(user)
            );

        } catch (AuthenticationException exception) {

            SecurityContextHolder.clearContext();

            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(
                    new AuthErrorResponse(
                        "AUTH_INVALID_CREDENTIALS",
                        "아이디 또는 비밀번호가 올바르지 않습니다."
                    )
                );
        }
    }

    @GetMapping("/me")
    public AuthUserResponse me(Authentication authentication) {

        AuthenticatedUser user =
            (AuthenticatedUser)
                authentication.getPrincipal();

        return AuthUserResponse.from(user);
    }
}