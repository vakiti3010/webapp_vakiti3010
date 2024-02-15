package com.csye6225.webapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class UserUpdateDTO {
    @JsonProperty("first_name")
    private String firstName;
    @JsonProperty("last_name")
    private String lastName;
    private String password;


    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Map<String, Object> getAttributes() {
        Map<String, Object> attributes = new HashMap<>();
        if (firstName != null) {
            attributes.put("first_name", firstName);
        }
        if (lastName != null) {
            attributes.put("last_name", lastName);
        }
        if (password != null) {
            attributes.put("password", password);
        }
        return attributes;
    }
}
