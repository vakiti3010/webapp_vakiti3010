package com.csye6225.webapp.controller;

import com.csye6225.webapp.exception.UserUpdateException;
import com.csye6225.webapp.model.User;
import com.csye6225.webapp.dto.UserResponseDTO;
import com.csye6225.webapp.dto.UserUpdateDTO;
import com.csye6225.webapp.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.lang.reflect.Field;
import java.net.URI;
import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("v20/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) throws JsonProcessingException {
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            return ResponseEntity.badRequest().body("Password cannot be empty.");
        }
        User createdUser = userService.createUser(user);
        UserResponseDTO response = mapToUserResponseDTO(createdUser);

        // Return 201 Created with location header
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdUser.getId()).toUri();

        return ResponseEntity.created(location).body(response);
    }


    @PutMapping("/self")
    public ResponseEntity<?> updateUser(@RequestBody String requestBody) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(requestBody);

            if (!isValidRequestBody(rootNode)) {
                return ResponseEntity.badRequest().body("Invalid fields in request");
            }

            UserUpdateDTO userUpdateDTO = objectMapper.treeToValue(rootNode, UserUpdateDTO.class);

            // Additional validation logic (if any)

            User updatedUser = userService.updateUser(currentUsername, userUpdateDTO);
            // Return 204 No Content if the update was successful
            return ResponseEntity.noContent().build();

        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().body("Invalid JSON format");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private boolean isValidRequestBody(JsonNode rootNode) {
        return rootNode.size() == 3 &&
                rootNode.has("first_name") &&
                rootNode.has("last_name") &&
                rootNode.has("password");
    }
    @GetMapping("/self")
    public ResponseEntity<?> getUserDetails(@RequestBody(required = false) Object requestBody, HttpServletRequest request) {
        // Check if there is a payload or query parameters in the request
        if (requestBody != null || !request.getParameterMap().isEmpty()) {
            return ResponseEntity.badRequest().body("GET requests should not include a request body or query parameters.");
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
