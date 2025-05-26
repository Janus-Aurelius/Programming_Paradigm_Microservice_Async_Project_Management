package com.pm.userservice.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.pm.commoncontracts.domain.UserRole;
import com.pm.userservice.model.User;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveMongoRepository<User, String> {
    Mono<User> findByEmail(String email);
    Flux<User> findByUsername(String username);
    Flux<User> findByRole(UserRole role);
}
