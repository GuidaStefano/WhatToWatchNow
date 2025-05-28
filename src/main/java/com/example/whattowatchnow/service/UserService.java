package com.example.whattowatchnow.service;

import com.example.whattowatchnow.domain.User;
import com.example.whattowatchnow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(User user) throws Exception {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new Exception("Email already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                new ArrayList<>() // Add authorities/roles here if needed
        );
    }

    // Optional: A method to find a user by email, which could be used by other services or for login checks if not relying solely on Spring Security
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User getUserProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
        // Ensure password is not sent
        user.setPassword(null);
        return user;
    }

    public User updateUserProfile(String userId, String newNickname, String newProfilePictureUrl) {
        // Ensure the authenticated user is updating their own profile
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName(); // This is the email
        User currentUser = userRepository.findByEmail(currentPrincipalName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + currentPrincipalName));

        if (!currentUser.getId().equals(userId)) {
            throw new SecurityException("User not authorized to update this profile.");
        }

        User userToUpdate = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        if (newNickname != null && !newNickname.isEmpty()) {
            userToUpdate.setNickname(newNickname);
        }
        if (newProfilePictureUrl != null) { // Allow empty string to clear profile picture
            userToUpdate.setProfilePicture(newProfilePictureUrl);
        }

        userRepository.save(userToUpdate);
        // Ensure password is not sent back
        userToUpdate.setPassword(null);
        return userToUpdate;
    }
}
