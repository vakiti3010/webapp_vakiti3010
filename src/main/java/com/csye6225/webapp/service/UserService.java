package com.csye6225.webapp.service;

import com.csye6225.webapp.exception.UserAlreadyExistsException;
import com.csye6225.webapp.exception.UserNotFoundException;
import com.csye6225.webapp.model.User;
import com.csye6225.webapp.dto.UserUpdateDTO;
import com.csye6225.webapp.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PubSubTemplate pubSubTemplate;


    private BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

    public User createUser(User user) throws JsonProcessingException {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException("User with email " + user.getUsername() + " already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Publish message to Pub/Sub
        String topicName = "verify_email";
        String message = serializeUser(user);
        pubSubTemplate.publish(topicName, message);
        return userRepository.save(user);
    }

    private String serializeUser(User user) throws JsonProcessingException {
        // Consider using a JSON serialization library like Jackson
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(user);
    }



    public User updateUser(String username, UserUpdateDTO updatedUserDetails) {
        User existingUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (updatedUserDetails.getFirstName() != null) {
            existingUser.setFirstName(updatedUserDetails.getFirstName());
        }
        if (updatedUserDetails.getLastName() != null) {
            existingUser.setLastName(updatedUserDetails.getLastName());
        }
        if (updatedUserDetails.getPassword() != null) {
            existingUser.setPassword(passwordEncoder.encode(updatedUserDetails.getPassword()));
        }

        return userRepository.save(existingUser);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
}


