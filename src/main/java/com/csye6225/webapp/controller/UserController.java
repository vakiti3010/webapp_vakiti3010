package com.csye6225.webapp.controller;

import com.csye6225.webapp.exception.UserUpdateException;
import com.csye6225.webapp.model.User;
import com.csye6225.webapp.dto.UserResponseDTO;
import com.csye6225.webapp.dto.UserUpdateDTO;
import com.csye6225.webapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("v1/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            return ResponseEntity.badRequest().body("Password cannot be empty.");
        }
        User createdUser = userService.createUser(user);
        UserResponseDTO response = mapToUserResponseDTO(createdUser);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/self")
    public ResponseEntity<?> updateUser(@RequestBody UserUpdateDTO userUpdateDTO) {
        // Retrieve the authenticated user's username from the SecurityContextHolder
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        for (Field field : userUpdateDTO.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                if (field.get(userUpdateDTO) != null &&
                        !field.getName().equals("firstName") &&
                        !field.getName().equals("lastName") &&
                        !field.getName().equals("password")) {

                    return ResponseEntity.badRequest().body("Invalid field in request");
                }
            } catch (IllegalAccessException e) {
                // Handle if needed
            }
        }

        try {
            User updatedUser = userService.updateUser(currentUsername, userUpdateDTO);
            // Return 204 No Content if the update was successful
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/self")
    public ResponseEntity<?> getUserDetails(@RequestBody(required = false) Object requestBody) {
        // Check if there is a payload attached to the request
        if (requestBody != null) {
            return ResponseEntity.badRequest().body("GET requests should not include a request body.");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        User user = userService.findByUsername(currentUsername);
        UserResponseDTO response = mapToUserResponseDTO(user);

        return ResponseEntity.ok(response);
    }


    private UserResponseDTO mapToUserResponseDTO(User user) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        UserResponseDTO responseDTO = new UserResponseDTO();
        responseDTO.setId(UUID.fromString(user.getId().toString())); // Assuming the ID is Long type
        responseDTO.setFirstName(user.getFirstName());
        responseDTO.setLastName(user.getLastName());
        responseDTO.setUsername(user.getUsername());
        responseDTO.setAccountCreated(user.getAccountCreated().format(formatter));
        if(user.getAccountUpdated()!=null){
            responseDTO.setAccountUpdated(user.getAccountUpdated().format(formatter));
        }
        else{
            responseDTO.setAccountUpdated(null);
        }

        return responseDTO;
    }


}
