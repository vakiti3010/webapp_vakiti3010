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

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("v1/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        User createdUser = userService.createUser(user);
        UserResponseDTO response = mapToUserResponseDTO(createdUser);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/self")
    public ResponseEntity<?> updateUser(@RequestBody UserUpdateDTO userUpdateDTO, Principal principal) {
        if (!principal.getName().equals(userUpdateDTO.getUsername())) {
            throw new UserUpdateException("You can only update your own account information.");
        }

        try {
            User updatedUser = userService.updateUser(userUpdateDTO.getUsername(), userUpdateDTO);
            UserResponseDTO response = mapToUserResponseDTO(updatedUser);
            return ResponseEntity.ok(response);
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
        if(user.getAccountUpdated()!=null){
            responseDTO.setAccountUpdated(user.getAccountUpdated().format(formatter));
        }
        else{
            responseDTO.setAccountUpdated(null);
        }

        return responseDTO;
    }


}
