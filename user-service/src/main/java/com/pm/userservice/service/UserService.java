package com.pm.userservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;

import com.pm.commoncontracts.dto.UserDto;
import com.pm.commoncontracts.envelope.EventEnvelope;
import com.pm.commoncontracts.events.user.UserCreatedEventPayload;
import com.pm.commoncontracts.events.user.UserDeletedEventPayload;
import com.pm.commoncontracts.events.user.UserUpdatedEventPayload;
import com.pm.userservice.config.MdcLoggingFilter;
import com.pm.userservice.exception.ConflictException;
import com.pm.userservice.exception.ResourceNotFoundException;
import com.pm.userservice.model.User;
import com.pm.userservice.repository.UserRepository;
import com.pm.userservice.utils.UserUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository repository;
    private final ReactiveKafkaProducerTemplate<String, EventEnvelope<?>> kafkaTemplate;

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${kafka.topic.user-events:user-events}")
    private String userEventsTopic;

    public UserService(UserRepository repository,
                       ReactiveKafkaProducerTemplate<String, EventEnvelope<?>> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    // Reactive helper to send Kafka events
    private <T> Mono<Void> sendEvent(String key, EventEnvelope<T> envelope) {
        return kafkaTemplate.send(userEventsTopic, key, envelope)
                .doOnError(e -> log.error("Failed to send event {}. CorrID: {}", envelope.eventType(), envelope.correlationId(), e))
                .then();
    }

    public Flux<UserDto> getUsers() {
        return repository.findAll()
                .map(UserUtils::entityToDto);
    }

    public Mono<UserDto> getUserById(String id) {
        return repository.findById(id)
                .map(UserUtils::entityToDto)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found: " + id)));
    }

    public Mono<UserDto> getUserByEmail(String email) {
        return repository.findByEmail(email)
                .map(UserUtils::entityToDto)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found: " + email)));
    }

    public Flux<UserDto> getUsersByRole(String role) {
        return repository.findByRole(role)
                .map(UserUtils::entityToDto)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("No users found for role: " + role)));
    }

    public Mono<UserDto> createUser(UserDto userDto) {
        return Mono.deferContextual(ctx ->
            repository.findByEmail(userDto.getEmail())
                .flatMap(existing -> Mono.error(new ConflictException("Email already in use: " + userDto.getEmail())))
                .switchIfEmpty(
                    repository.insert(UserUtils.dtoToEntity(userDto))
                        .flatMap(saved -> {
                            String corr = ctx.getOrDefault(MdcLoggingFilter.CORRELATION_ID_CONTEXT_KEY, "N/A-create");
                            UserCreatedEventPayload payload = new UserCreatedEventPayload(UserUtils.entityToDto(saved));
                            EventEnvelope<UserCreatedEventPayload> envelope = new EventEnvelope<>(corr, UserCreatedEventPayload.EVENT_TYPE, serviceName, payload);
                            return sendEvent(saved.getId(), envelope).thenReturn(saved);
                        })
                )
        ).map(u -> UserUtils.entityToDto((User) u)); // Explicit cast
    }

    public Mono<UserDto> updateUser(String id, UserDto dto) {
        return Mono.deferContextual(ctx ->
            // Ensure email is unique (other than current user)
            repository.findByEmail(dto.getEmail())
                .flatMap(existing -> {
                    if (!existing.getId().equals(id)) {
                        return Mono.error(new ConflictException("Email already in use: " + dto.getEmail()));
                    }
                    return Mono.empty();
                })
                .then(repository.findById(id)
                    .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found: " + id)))
                )
                .flatMap(user -> {
                    user.setEmail(dto.getEmail());
                    // Only update passwordHash if a new password is provided
                    if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
                        user.setPasswordHash(hashPassword(dto.getPassword())); // Implement hashPassword
                    }
                    // Update roles
                    if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
                        user.setRoles(dto.getRoles());
                    } else if (dto.getRole() != null) {
                        user.setRoles(java.util.List.of(dto.getRole()));
                    }
                    user.setFirstName(dto.getFirstName());
                    user.setLastName(dto.getLastName());
                    user.setEnabled(dto.isEnabled());
                    user.setLocked(dto.isLocked());
                    user.setProfilePictureUrl(dto.getProfilePictureUrl());
                    // ...other fields as needed
                    return repository.save(user);
                })
                .flatMap(saved -> {
                    String corr = ctx.getOrDefault(MdcLoggingFilter.CORRELATION_ID_CONTEXT_KEY, "N/A-update");
                    UserUpdatedEventPayload payload = new UserUpdatedEventPayload(UserUtils.entityToDto(saved));
                    EventEnvelope<UserUpdatedEventPayload> envelope = new EventEnvelope<>(corr, UserUpdatedEventPayload.EVENT_TYPE, serviceName, payload);
                    return sendEvent(saved.getId(), envelope).thenReturn(saved);
                })
                .map(UserUtils::entityToDto)
        );
    }

    public Mono<UserDto> updateUserProfile(String id, UserDto profileDto) {
        return Mono.deferContextual(ctx ->
            repository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found: " + id)))
                .flatMap(user -> {
                    boolean changed = false;
                    if (profileDto.getFirstName() != null && !profileDto.getFirstName().equals(user.getFirstName())) {
                        user.setFirstName(profileDto.getFirstName());
                        changed = true;
                    }
                    if (profileDto.getLastName() != null && !profileDto.getLastName().equals(user.getLastName())) {
                        user.setLastName(profileDto.getLastName());
                        changed = true;
                    }
                    if (!changed) return Mono.just(user); // No changes
                    return repository.save(user);
                })
                .flatMap(saved -> {
                    String corr = ctx.getOrDefault(MdcLoggingFilter.CORRELATION_ID_CONTEXT_KEY, "N/A-profile-update");
                    UserUpdatedEventPayload payload = new UserUpdatedEventPayload(UserUtils.entityToDto(saved));
                    EventEnvelope<UserUpdatedEventPayload> envelope = new EventEnvelope<>(corr, UserUpdatedEventPayload.EVENT_TYPE, serviceName, payload);
                    return sendEvent(saved.getId(), envelope).thenReturn(saved);
                })
                .map(UserUtils::entityToDto)
        );
    }

    public Mono<Void> changeUserPassword(String id, String newPassword) {
        return Mono.deferContextual(ctx ->
            repository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found: " + id)))
                .flatMap(user -> {
                    if (newPassword == null || newPassword.isBlank() || newPassword.equals(user.getPasswordHash())) {
                        return Mono.empty(); // No change
                    }
                    user.setPasswordHash(hashPassword(newPassword)); // Implement hashPassword
                    return repository.save(user);
                })
                .flatMap(saved -> {
                    String corr = ctx.getOrDefault(MdcLoggingFilter.CORRELATION_ID_CONTEXT_KEY, "N/A-password-change");
                    UserUpdatedEventPayload payload = new UserUpdatedEventPayload(UserUtils.entityToDto(saved));
                    EventEnvelope<UserUpdatedEventPayload> envelope = new EventEnvelope<>(corr, UserUpdatedEventPayload.EVENT_TYPE, serviceName, payload);
                    return sendEvent(saved.getId(), envelope);
                })
        );
    }

    public Mono<UserDto> deleteUser(String id) {
        return Mono.deferContextual(ctx ->
            repository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found: " + id)))
                // Delete then return the deleted entity
                .flatMap(user -> repository.delete(user).thenReturn(user))
                // Publish deletion event and map to DTO
                .flatMap(deleted -> {
                    String corr = ctx.getOrDefault(MdcLoggingFilter.CORRELATION_ID_CONTEXT_KEY, "N/A-delete");
                    UserDeletedEventPayload payload = new UserDeletedEventPayload(UserUtils.entityToDto(deleted));
                    EventEnvelope<UserDeletedEventPayload> envelope = new EventEnvelope<>(corr, UserDeletedEventPayload.EVENT_TYPE, serviceName, payload);
                    return sendEvent(deleted.getId(), envelope).thenReturn(deleted);
                })
                .map(UserUtils::entityToDto) // Explicit cast
        );
    }

    // Placeholder for password hashing logic
    private String hashPassword(String rawPassword) {
        // TODO: Replace with your actual password hashing implementation (e.g., BCrypt)
        return "HASHED_" + rawPassword;
    }
}
