package com.pm.commoncontracts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@Builder
@Data
public class UserDto {
    private String id;
    private String username;
    private String password;
    private String email;
    private String name;
    private String role;
    private String firstName;
    private String lastName;

    public UserDto() {}

    public String getFirstName() { return this.firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return this.lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

}
