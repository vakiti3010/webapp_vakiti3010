package com.csye6225.webapp.service;

import com.csye6225.webapp.exception.UnauthorizedException;
import com.csye6225.webapp.exception.UserAlreadyExistsException;
import com.csye6225.webapp.exception.UserNotFoundException;
import com.csye6225.webapp.model.User;
import com.csye6225.webapp.dto.UserUpdateDTO;
import com.csye6225.webapp.model.VerificationToken;
import com.csye6225.webapp.repository.UserRepository;
import com.csye6225.webapp.repository.VerificationTokenRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.hibernate.service.spi.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;

import jakarta.transaction.Transactional;
import org.springframework.web.server.ResponseStatusException;


@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PubSubTemplate pubSubTemplate;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);


    private BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

    public User createUser(User user) throws JsonProcessingException {
        logger.info("Attempting to create user with username: {}", user.getUsername());

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            logger.warn("User creation failed: User with email {} already exists", user.getUsername());
            throw new UserAlreadyExistsException("User with email " + user.getUsername() + " already exists");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        try {
            publishMessageToPubSub(user);
            logger.info("User data published to Pub/Sub for username: {}", user.getUsername());
        } catch (Exception e) {
            logger.error("Error while publishing message to Pub/Sub for username: {}", user.getUsername(), e);
            // Rethrow, return null, or handle the exception as needed
            throw new ServiceException("Failed to publish user data for " + user.getUsername(), e);
        }

        try {
            User savedUser = userRepository.save(user);
            logger.info("User created successfully with username: {}", user.getUsername());
            return savedUser;
        } catch (Exception e) {
            logger.error("Error while saving user to the database for username: {}", user.getUsername(), e);
            // Rethrow, return null, or handle the exception as needed
            throw new ServiceException("Failed to save user to the database for " + user.getUsername(), e);
        }
    }
    private String serializeUser(User user) throws JsonProcessingException {
        // Consider using a JSON serialization library like Jackson
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode userNode = mapper.valueToTree(user);

        // Remove the password attribute
        userNode.remove("password");

        return mapper.writeValueAsString(userNode);
    }

    private void publishMessageToPubSub(User user) throws JsonProcessingException {
        String projectId = "vakiti-dev";
        String topicId = "verify_email";
        String jsonData = serializeUser(user);

        ProjectTopicName topicName = ProjectTopicName.of(projectId, topicId);
        Publisher publisher = null;

        try {

            logger.info("Publishing message to topic {}", topicId);
            logger.info(jsonData);
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
        logger.info("Attempting to update user with username: {}", username);

        Optional<VerificationToken> token = tokenRepository.findByEmail(username);
        if (token.isEmpty() || !token.get().isVerified()) {
            logger.warn("User update failed: User {} is not verified or token not found", username);
            throw new UnauthorizedException("User is not verified");
        }

        User existingUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isUpdated = false;
        if (updatedUserDetails.getFirstName() != null) {
            existingUser.setFirstName(updatedUserDetails.getFirstName());
            isUpdated = true;
        }
        if (updatedUserDetails.getLastName() != null) {
            existingUser.setLastName(updatedUserDetails.getLastName());
            isUpdated = true;
        }
        if (updatedUserDetails.getPassword() != null) {
            existingUser.setPassword(passwordEncoder.encode(updatedUserDetails.getPassword()));
            isUpdated = true;
        }

        if (isUpdated) {
            User updatedUser = userRepository.save(existingUser);
            logger.info("User successfully updated with username: {}", username);
            return updatedUser;
        } else {
            logger.info("No changes made to the user with username: {}", username);
            return existingUser;
        }
    }


    public User findByUsername(String username) {
        Optional<VerificationToken> token = tokenRepository.findByEmail(username);
        if (token.isEmpty() || !token.get().isVerified()) {
            throw new UnauthorizedException("User is not verified");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Transactional
    public boolean verifyUser(String token) {
        logger.info("Attempting to verify token: {}", token);
        return tokenRepository.findByToken(token)
                .filter(verificationToken -> {
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime expiration = verificationToken.getExpiration();

                    logger.info("Token: {}, Expiration time: {}, Current time: {}", token, expiration, now);

                    boolean isValid = !verificationToken.isVerified() && expiration.isAfter(now);
                    if (!isValid) {
                        logger.warn("Token verification failed for token: {}. Either already verified or expired.", token);
                    }
                    return isValid;
                })
                .map(verificationToken -> {
                    // Update the token's verified status
                    verificationToken.setVerified(true);
                    tokenRepository.save(verificationToken);
                    logger.info("Token verified successfully: {}", token);
                    return true;
                })
                .orElseGet(() -> {
                    logger.warn("Token not found or invalid for token: {}", token);
                    return false;
                });
    }
}


