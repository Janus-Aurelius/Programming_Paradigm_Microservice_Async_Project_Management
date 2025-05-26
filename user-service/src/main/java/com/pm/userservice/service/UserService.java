package com.pm.userservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;

import com.pm.commoncontracts.domain.UserRole;
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

    private <T> Mono<Void> sendEvent(String key, EventEnvelope<T> envelope) {
        return kafkaTemplate.send(userEventsTopic, key, envelope)
                .doOnError(e -> log.error("Failed to send event {}. CorrID: {}", envelope.eventType(), envelope.correlationId(), e))
                .then();
    }

    public Flux<UserDto> getUsers() {
        return repository.findAll()
                .map(UserUtils::toDto);
    }

    public Mono<UserDto> getUserById(String id) {
        return repository.findById(id)
                .map(UserUtils::toDto)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found with id: " + id)));
    }    public Mono<UserDto> getUserByEmail(String email) {
        return repository.findByEmail(email)
                .map(UserUtils::toDto)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found with email: " + email)));
    }    public Mono<UserDto> getUserByEmailForAuthentication(String email) {
        return repository.findByEmail(email)
                .map(UserUtils::toDtoWithPassword)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found with email: " + email)));
    }

    public Mono<User> getUserEntityByEmailForAuthentication(String email) {
        return repository.findByEmail(email)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found with email: " + email)));
    }    public Flux<UserDto> getUsersByRole(UserRole role) {
        return repository.findByRole(role)
                .map(UserUtils::toDto)
                .switchIfEmpty(Flux.empty());
    }

    public Mono<UserDto> createUser(UserDto userDto) {
        return Mono.deferContextual(ctx ->
            repository.findByUsername(userDto.getUsername()).next()
                .flatMap(existingUser -> Mono.error(new ConflictException("Username already exists: " + userDto.getUsername())))
                .switchIfEmpty(Mono.defer(() -> repository.findByEmail(userDto.getEmail())
                    .flatMap(existingUser -> Mono.error(new ConflictException("Email already in use: " + userDto.getEmail())))))                .switchIfEmpty(Mono.defer(() -> {
                    User user = UserUtils.toEntity(userDto);
                    if (user.getRole() == null) {
                        user.setRole(UserRole.ROLE_USER); // Set default role
                    }
                    return repository.insert(user)
                        .flatMap(savedUser -> {
                            String corr = ctx.getOrDefault(MdcLoggingFilter.CORRELATION_ID_CONTEXT_KEY, "N/A-create");
                            UserCreatedEventPayload payload = new UserCreatedEventPayload(UserUtils.toDto(savedUser));
                            EventEnvelope<UserCreatedEventPayload> envelope = new EventEnvelope<>(corr, UserCreatedEventPayload.EVENT_TYPE, serviceName, payload);
                            return sendEvent(savedUser.getId(), envelope).thenReturn(savedUser);
                        });
                }))
                .cast(User.class)
        ).map(UserUtils::toDto);
    }

    public Mono<UserDto> updateUser(String id, UserDto userDto) {
        return Mono.deferContextual(ctx ->
            repository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found: " + id)))
                .flatMap(existingUser -> {
                    Mono<User> emailConflictCheck = Mono.just(existingUser);
                    if (userDto.getEmail() != null && !userDto.getEmail().equalsIgnoreCase(existingUser.getEmail())) {
                        emailConflictCheck = repository.findByEmail(userDto.getEmail())
                            .flatMap(conflictingUser -> !conflictingUser.getId().equals(id) ? 
                                Mono.error(new ConflictException("Email already in use: " + userDto.getEmail())) :
                                Mono.just(existingUser)
                            )
                            .switchIfEmpty(Mono.just(existingUser));
                    }
                    return emailConflictCheck;
                })
                .flatMap(existingUser -> {
                    Mono<User> usernameConflictCheck = Mono.just(existingUser);
                    if (userDto.getUsername() != null && !userDto.getUsername().equalsIgnoreCase(existingUser.getUsername())) {
                         usernameConflictCheck = repository.findByUsername(userDto.getUsername()).next()
                            .flatMap(conflictingUser -> !conflictingUser.getId().equals(id) ? 
                                Mono.error(new ConflictException("Username already exists: " + userDto.getUsername())) :
                                Mono.just(existingUser)
                            )
                            .switchIfEmpty(Mono.just(existingUser));
                    }
                    return usernameConflictCheck;
                })
                .flatMap(user -> {
                    User userToSave = UserUtils.updateUserFromDto(user, userDto);
                    return repository.save(userToSave);
                })
                .flatMap(savedUser -> {
                    String corr = ctx.getOrDefault(MdcLoggingFilter.CORRELATION_ID_CONTEXT_KEY, "N/A-update");
                    UserUpdatedEventPayload payload = new UserUpdatedEventPayload(UserUtils.toDto(savedUser));
                    EventEnvelope<UserUpdatedEventPayload> envelope = new EventEnvelope<>(corr, UserUpdatedEventPayload.EVENT_TYPE, serviceName, payload);
                    return sendEvent(savedUser.getId(), envelope).thenReturn(savedUser);
                })
                .map(UserUtils::toDto)
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
                    if (!changed) return Mono.just(user);
                    return repository.save(user);
                })
                .flatMap(savedUser -> {
                    String corr = ctx.getOrDefault(MdcLoggingFilter.CORRELATION_ID_CONTEXT_KEY, "N/A-profile-update");
                    UserUpdatedEventPayload payload = new UserUpdatedEventPayload(UserUtils.toDto(savedUser));
                    EventEnvelope<UserUpdatedEventPayload> envelope = new EventEnvelope<>(corr, UserUpdatedEventPayload.EVENT_TYPE, serviceName, payload);
                    return sendEvent(savedUser.getId(), envelope).thenReturn(savedUser);
                })
                .map(UserUtils::toDto)
        );
    }

    public Mono<Void> changeUserPassword(String id, String newPassword) {
        return Mono.deferContextual(ctx ->
            repository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found: " + id)))
                .flatMap(user -> {
                    if (newPassword == null || newPassword.isBlank()) {
                        return Mono.error(new IllegalArgumentException("New password cannot be blank."));
                    }
                    user.setHashedPassword(UserUtils.getPasswordEncoder().encode(newPassword.trim()));
                    return repository.save(user);
                })
                .flatMap(savedUser -> {
                    String corr = ctx.getOrDefault(MdcLoggingFilter.CORRELATION_ID_CONTEXT_KEY, "N/A-password-change");
                    UserUpdatedEventPayload payload = new UserUpdatedEventPayload(UserUtils.toDto(savedUser));
                    EventEnvelope<UserUpdatedEventPayload> envelope = new EventEnvelope<>(corr, UserUpdatedEventPayload.EVENT_TYPE, serviceName, payload);
                    return sendEvent(savedUser.getId(), envelope);
                })
        );
    }

    public Mono<UserDto> deleteUser(String id) {
        return Mono.deferContextual(ctx ->
            repository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found: " + id)))
                .flatMap(user -> repository.delete(user).thenReturn(user))
                .flatMap(deletedUser -> {
                    String corr = ctx.getOrDefault(MdcLoggingFilter.CORRELATION_ID_CONTEXT_KEY, "N/A-delete");
                    UserDeletedEventPayload payload = new UserDeletedEventPayload(UserUtils.toDto(deletedUser));
                    EventEnvelope<UserDeletedEventPayload> envelope = new EventEnvelope<>(corr, UserDeletedEventPayload.EVENT_TYPE, serviceName, payload);
                    return sendEvent(deletedUser.getId(), envelope).thenReturn(deletedUser);
                })
                .map(UserUtils::toDto)
        );
    }
}
