package com.csye6225.webapp.service;

import com.csye6225.webapp.repository.UserRepository;
// Remove the aliasing in import
import com.csye6225.webapp.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> customUserOpt = userRepository.findByUsername(username);

        if (!customUserOpt.isPresent()) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }

        User customUser = customUserOpt.get();

        return new org.springframework.security.core.userdetails.User(
                customUser.getUsername(),
                customUser.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
