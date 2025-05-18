package com.pm.commentservice.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import com.pm.commentservice.model.Comment;
import com.pm.commoncontracts.domain.ParentType;

import reactor.core.publisher.Flux;

@Repository
public interface CommentRepository extends ReactiveMongoRepository<Comment, String> {    // Find comments for a specific parent entity, ordered by creation time
    Flux<Comment> findByParentIdAndParentTypeOrderByCreatedAtAsc(String parentId, ParentType parentType);
}
