package com.pm.projectservice.repository;

import com.pm.projectservice.model.Project;
import com.pm.commoncontracts.domain.ProjectStatus;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface ProjectRepository extends ReactiveMongoRepository<Project, String> {
    Flux<Project> findByName(String name);
    Flux<Project> findByCreatedBy(String createdBy);
    Flux<Project> findByStatus(ProjectStatus status);
}