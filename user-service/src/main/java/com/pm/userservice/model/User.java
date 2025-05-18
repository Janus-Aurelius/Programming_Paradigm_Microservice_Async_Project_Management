package com.pm.userservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection="users")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class User {
    @Id
    private String id;
    private String username;
    private String password;
    private String name;
    private String email;
    private String role;
    private String firstName;
    private String lastName;

}
