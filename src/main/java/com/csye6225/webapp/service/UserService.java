package com.csye6225.webapp.service;

import com.csye6225.webapp.exception.UserAlreadyExistsException;
import com.csye6225.webapp.exception.UserNotFoundException;
import com.csye6225.webapp.model.User;
import com.csye6225.webapp.dto.UserUpdateDTO;
import com.csye6225.webapp.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;


@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PubSubTemplate pubSubTemplate;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);


    private BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

    public User createUser(User user) throws JsonProcessingException {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException("User with email " + user.getUsername() + " already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        publishMessageToPubSub(user);
        return userRepository.save(user);
    }

    private String serializeUser(User user) throws JsonProcessingException {
        // Consider using a JSON serialization library like Jackson
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(user);
    }

    private void publishMessageToPubSub(User user) throws JsonProcessingException {
        String projectId = "vakiti-dev";
        String topicId = "verify_email";
        String jsonData = serializeUser(user);

        ProjectTopicName topicName = ProjectTopicName.of(projectId, topicId);
        Publisher publisher = null;

        try {

            logger.info("Publishing message to topic {}", topicId);

            publisher = Publisher.newBuilder(topicName).build();
            ByteString data = ByteString.copyFromUtf8(jsonData);
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();

            publisher.publish(pubsubMessage).get(); // Or handle asynchronously
            logger.info("Message published successfully to topic {}", topicId);
        } catch (Exception e) {
            logger.error("Error publishing message to Pub/Sub", e);

        } finally {
            if (publisher != null) {
                try {
                    publisher.shutdown();
                    logger.info("Publisher shut down successfully");
                } catch (Exception e) {
                    logger.error("Error shutting down the publisher", e);
                }
            }
        }
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


