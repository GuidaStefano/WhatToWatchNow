package com.example.whattowatchnow.controller;

import com.example.whattowatchnow.domain.User;
import com.example.whattowatchnow.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        try {
            User registeredUser = userService.registerUser(user);
            // Avoid returning the password in the response
            registeredUser.setPassword(null); 
            return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Login endpoint - Spring Security will typically handle this.
    // If custom login logic or response is needed, it can be added here.
    // For now, we'll rely on Spring Security's default behavior.
    // A simple POST /login endpoint might be implicitly handled by Spring Security
    // or you might need a placeholder if you're not using formLogin.

    @GetMapping("/me")
    public ResponseEntity<com.example.whattowatchnow.dto.UserProfileDto> getCurrentUserProfile() {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        User currentUser = userService.findByEmail(userEmail)
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found with email: " + userEmail));
        
        User userProfile = userService.getUserProfile(currentUser.getId());
        com.example.whattowatchnow.dto.UserProfileDto userProfileDto = new com.example.whattowatchnow.dto.UserProfileDto(
            userProfile.getId(),
            userProfile.getNickname(),
            userProfile.getEmail(),
            userProfile.getProfilePicture()
        );
        return ResponseEntity.ok(userProfileDto);
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateUserProfile(@RequestBody com.example.whattowatchnow.dto.UserProfileDto profileUpdateDto) {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        User currentUser = userService.findByEmail(userEmail)
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found with email: " + userEmail));

        try {
            User updatedUser = userService.updateUserProfile(
                currentUser.getId(),
                profileUpdateDto.getNickname(),
                profileUpdateDto.getProfilePicture()
            );
            com.example.whattowatchnow.dto.UserProfileDto updatedUserProfileDto = new com.example.whattowatchnow.dto.UserProfileDto(
                updatedUser.getId(),
                updatedUser.getNickname(),
                updatedUser.getEmail(),
                updatedUser.getProfilePicture()
            );
            return ResponseEntity.ok(updatedUserProfileDto);
        } catch (SecurityException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
