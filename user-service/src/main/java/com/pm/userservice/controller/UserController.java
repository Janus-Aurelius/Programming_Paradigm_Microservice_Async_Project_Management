package com.pm.userservice.controller;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.pm.commoncontracts.domain.UserRole;
import com.pm.commoncontracts.dto.UserDto;
import com.pm.userservice.dto.JwtResponse;
import com.pm.userservice.dto.LoginRequest;
import com.pm.userservice.exception.ConflictException;
import com.pm.userservice.exception.ResourceNotFoundException;
import com.pm.userservice.service.UserService;
import com.pm.userservice.utils.JwtUtil;
import com.pm.userservice.utils.UserUtils;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("")
public class UserController {

    @Value("${jwt.enabled:true}")
    private boolean jwtEnabled;

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public UserController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, 'USER_READ')")
    public Flux<UserDto> getAllUsers() {
        log.info("Fetching all users");
        return userService.getUsers();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(#id, 'USER_READ')")
    public Mono<ResponseEntity<UserDto>> getUserById(@PathVariable String id) {
        log.info("Fetching user by id: {}", id);
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasPermission(#email, 'USER_READ')")
    public Mono<ResponseEntity<UserDto>> getUserByEmail(@PathVariable String email) {
        log.info("Fetching user by email: {}", email);
        return userService.getUserByEmail(email)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasPermission(null, 'USER_READ')")
    public Flux<UserDto> getUsersByRole(@PathVariable String role) {
        log.info("Fetching users by role: {}", role);
        return userService.getUsersByRole(UserRole.valueOf(role));
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'USER_CREATE')")
    public Mono<ResponseEntity<UserDto>> createUser(@Valid @RequestBody UserDto userDto, UriComponentsBuilder uriBuilder) {
        log.info("Creating new user: {}", userDto.getEmail());
        return userService.createUser(userDto)
                .map(createdUser -> {
                    URI location = uriBuilder.path("/api/users/{id}")
                            .buildAndExpand(createdUser.getId())
                            .toUri();
                    return ResponseEntity.created(location).body(createdUser);
                })
                .onErrorResume(ConflictException.class, e
                        -> Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build())
                );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(#id, 'USER_UPDATE')")
    public Mono<ResponseEntity<UserDto>> updateUser(@PathVariable String id, @Valid @RequestBody UserDto userDto) {
        log.info("Updating user with id: {}", id);
        return userService.updateUser(id, userDto)
                .map(ResponseEntity::ok)
                .onErrorResume(ResourceNotFoundException.class, e
                        -> Mono.just(ResponseEntity.notFound().build())
                )
                .onErrorResume(ConflictException.class, e
                        -> Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build())
                );
    }

    @PatchMapping("/{id}/profile")
    @PreAuthorize("hasPermission(#id, 'USER_UPDATE')")
    public Mono<ResponseEntity<UserDto>> updateProfile(@PathVariable String id, @RequestBody UserDto profileDto) {
        log.info("Updating profile for user with id: {}", id);
        return userService.updateUserProfile(id, profileDto)
                .map(ResponseEntity::ok)
                .onErrorResume(ResourceNotFoundException.class, e
                        -> Mono.just(ResponseEntity.notFound().build())
                );
    }

    @PatchMapping("/{id}/password")
    @PreAuthorize("hasPermission(#id, 'USER_UPDATE')")
    public Mono<ResponseEntity<Void>> changePassword(@PathVariable String id, @RequestBody UserDto passwordDto) {
        log.info("Changing password for user with id: {}", id);
        return userService.changeUserPassword(id, passwordDto.getPassword())
                .thenReturn(ResponseEntity.noContent().<Void>build())
                .onErrorResume(ResourceNotFoundException.class, e
                        -> Mono.just(ResponseEntity.notFound().build())
                );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(#id, 'USER_DELETE')")
    public Mono<ResponseEntity<Void>> deleteUser(@PathVariable String id) {
        log.info("Deleting user with id: {}", id);
        return userService.deleteUser(id)
                .thenReturn(ResponseEntity.noContent().<Void>build())
                .onErrorResume(ResourceNotFoundException.class, e
                        -> Mono.just(ResponseEntity.notFound().build())
                );
    }

    @PostMapping("/auth/login")
    public Mono<ResponseEntity<JwtResponse>> login(@RequestBody LoginRequest loginRequest) {
        return userService.getUserEntityByEmailForAuthentication(loginRequest.getEmail())
                .flatMap(user -> {
                    if (loginRequest.getPassword().equals(user.getPassword())) {
                        String role = UserUtils.getRoleAsString(user);
                        String token = jwtUtil.generateToken(
                                user.getId(),
                                user.getEmail(),
                                role,
                                com.pm.userservice.config.JwtConfig.JWT_EXPIRATION_MS
                        );
                        // Convert to DTO for response (without password)
                        UserDto userDto = UserUtils.toDto(user);
                        return Mono.just(ResponseEntity.ok(new JwtResponse(token, userDto)));
                    } else {
                        log.warn("Password mismatch for user: {}", loginRequest.getEmail());
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).<JwtResponse>build());
                    }
                })
                .onErrorResume(ResourceNotFoundException.class, e -> {
                    log.warn("User not found: {}", loginRequest.getEmail());
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).<JwtResponse>build());
                })
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.UNAUTHORIZED).<JwtResponse>build());
    }
}
