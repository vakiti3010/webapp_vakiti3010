package com.csye6225.webapp.controller;

import com.csye6225.webapp.models.User;
import com.csye6225.webapp.models.UserResponseDTO;
import com.csye6225.webapp.models.UserUpdateDTO;
import com.csye6225.webapp.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("v1/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        logger.info("Attempting to create user: {}", user.getUsername());
        User createdUser = userService.createUser(user);
        return ResponseEntity.ok(createdUser); // Or use ResponseEntity.created for a 201 status
    }

    @PutMapping("/self")
    public ResponseEntity<?> updateUser(@RequestBody UserUpdateDTO userUpdateDTO, Principal principal) {
        if (!principal.getName().equals(userUpdateDTO.getUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only update your own account information.");
        }

        try {
            User updatedUser = userService.updateUser(userUpdateDTO.getUsername(), userUpdateDTO);
            updatedUser.setPassword(null); // Do not return the password
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/self")
    public ResponseEntity<UserResponseDTO> getUserDetails() {
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
        responseDTO.setAccountUpdated(user.getAccountUpdated().format(formatter));

        return responseDTO;
    }


}
