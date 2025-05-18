package com.pm.taskservice.repository;

import com.pm.taskservice.model.Task;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface TaskRepository extends ReactiveMongoRepository<Task, String> {
    Flux<Task> findByProjectId(String projectId);
    Flux<Task> findByAssigneeId(String assigneeId); // Updated from findByAssignedTo
}
