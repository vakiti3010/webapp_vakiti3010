package com.csye6225.webapp.controller;

import com.csye6225.webapp.dto.UserResponseDTO;
import com.csye6225.webapp.dto.UserUpdateDTO;
import com.csye6225.webapp.model.User;
import com.csye6225.webapp.model.VerificationToken;
import com.csye6225.webapp.repository.VerificationTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class UserIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private VerificationTokenRepository tokenRepository;


    @Test
    public void testCreateUserAndValidate() {
        // Create user
        User newUser = new User();
        newUser.setFirstName("First");
        newUser.setLastName("Last");
        newUser.setPassword("password");
        newUser.setUsername("username");
        ResponseEntity<UserResponseDTO> createResponse = restTemplate.postForEntity("/v20/user", newUser, UserResponseDTO.class);

        // Create and save a dummy verification token
        VerificationToken dummyToken = new VerificationToken();
        dummyToken.setToken("dummyToken");
        dummyToken.setEmail(newUser.getUsername()); // Assuming username is the email
        dummyToken.setExpiration(LocalDateTime.now().plusDays(1)); // Set future expiration
        dummyToken.setVerified(true); // Mark as verified
        tokenRepository.save(dummyToken);

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());

        UserResponseDTO createdUser = createResponse.getBody();
        assertNotNull(createdUser);
        assertNotNull(createdUser.getUsername());

        // Set up basic authentication
        String username = "username";
        String password = "password";
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, password);

        // Validate user exists
        //UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/v20/user/self");
        ResponseEntity<UserResponseDTO> getResponse = restTemplate.exchange(
                "/v20/user/self",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                UserResponseDTO.class
        );

        if (getResponse.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            System.out.println("Unauthorized: " + getResponse.getBody());
        }

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        UserResponseDTO fetchedUser = getResponse.getBody();
        assertNotNull(fetchedUser);
        assertEquals(newUser.getUsername(), fetchedUser.getUsername());
    }


    @Test
    public void testUpdateUserAndValidate() {
        // Create a new user
        User newUser = new User();
        newUser.setFirstName("First");
        newUser.setLastName("Last");
        newUser.setPassword("password");
        newUser.setUsername("username1");

        // Create and save a dummy verification token
        VerificationToken dummyToken = new VerificationToken();
        dummyToken.setToken("dummyToken1");
        dummyToken.setEmail(newUser.getUsername()); // Assuming username is the email
        dummyToken.setExpiration(LocalDateTime.now().plusDays(1)); // Set future expiration
        dummyToken.setVerified(true); // Mark as verified
        tokenRepository.save(dummyToken);

        ResponseEntity<UserResponseDTO> createResponse = restTemplate.postForEntity("/v20/user", newUser, UserResponseDTO.class);
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());

        UserResponseDTO createdUser = createResponse.getBody();
        assertNotNull(createdUser);
        assertNotNull(createdUser.getUsername());

        // Set up basic authentication
        String username = "username1";
        String password = "password";
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, password);

        // Update the user's information
        UserUpdateDTO userUpdateDTO = new UserUpdateDTO();
        userUpdateDTO.setFirstName("UpdatedFirstName");
        userUpdateDTO.setLastName("UpdatedLastName");
        userUpdateDTO.setPassword("password");

        ResponseEntity<UserResponseDTO> updateResponse = restTemplate.exchange(
                "/v20/user/self",
                HttpMethod.PUT,
                new HttpEntity<>(userUpdateDTO, headers),
                UserResponseDTO.class
        );

        assertEquals(HttpStatus.NO_CONTENT, updateResponse.getStatusCode());

        // Validate that the user's information was updated
        ResponseEntity<UserResponseDTO> getResponse = restTemplate.exchange(
                "/v20/user/self",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                UserResponseDTO.class
        );

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());

        UserResponseDTO updatedUser = getResponse.getBody();
        assertNotNull(updatedUser);
        assertEquals(userUpdateDTO.getFirstName(), updatedUser.getFirstName());
        assertEquals(userUpdateDTO.getLastName(), updatedUser.getLastName());
        assertEquals(createdUser.getUsername(), updatedUser.getUsername());
    }
}

