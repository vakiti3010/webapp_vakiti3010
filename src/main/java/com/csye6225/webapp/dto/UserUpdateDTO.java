package com.csye6225.webapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.lang.reflect.Field;
import java.util.Arrays;
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

    // Checks if all required fields are present
    public boolean hasRequiredFields() {
        return firstName != null && lastName != null && password != null;
    }

    // Checks for any additional invalid fields
    public boolean hasOnlyValidFields() {
        Map<String, Boolean> allowedFields = new HashMap<>();
        allowedFields.put("firstName", true);
        allowedFields.put("lastName", true);
        allowedFields.put("password", true);

        for (Field field : this.getClass().getDeclaredFields()) {
            if (!allowedFields.containsKey(field.getName())) {
                return false; // Invalid field found
            }
        }
        return true; // Only valid fields found
    }

}
