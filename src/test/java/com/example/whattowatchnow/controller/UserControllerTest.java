package com.example.whattowatchnow.controller;

import com.example.whattowatchnow.domain.User;
import com.example.whattowatchnow.dto.UserProfileDto;
import com.example.whattowatchnow.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;
    
    // We also need to mock UserDetailsService if Spring Security is fully engaged,
    // but for WebMvcTest focused on UserController, @WithMockUser often suffices for authorization.
    // If SecurityConfig brings in UserDetailsService directly, it might need mocking.
    // UserService itself implements UserDetailsService, so @MockBean for UserService should cover it.


    @Autowired
    private ObjectMapper objectMapper;

    private User user;
    private UserProfileDto userProfileDto;

    @BeforeEach
    void setUp() {
        user = new User("testUser", "test@example.com", "password123", "profile.jpg");
        user.setId("userId123");

        userProfileDto = new UserProfileDto(user.getId(), user.getNickname(), user.getEmail(), user.getProfilePicture());
    }

    @Test
    void registerUser_success() throws Exception {
        User registeredUser = new User(user.getNickname(), user.getEmail(), null, user.getProfilePicture()); // Password null in response
        registeredUser.setId(user.getId());

        when(userService.registerUser(any(User.class))).thenReturn(registeredUser);

        mockMvc.perform(post("/api/users/register")
                .with(csrf()) // Add CSRF token for POST requests if CSRF is enabled (default in Spring Security)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new User(user.getNickname(), user.getEmail(), "password123", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.nickname").value(user.getNickname()))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void registerUser_emailExists_returnsBadRequest() throws Exception {
        when(userService.registerUser(any(User.class))).thenThrow(new Exception("Email already exists"));

        mockMvc.perform(post("/api/users/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new User(user.getNickname(), user.getEmail(), "password123", null))))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Email already exists"));
    }

    @Test
    @WithMockUser(username = "test@example.com") // Mock an authenticated user
    void getCurrentUserProfile_success() throws Exception {
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userService.getUserProfile(user.getId())).thenReturn(user); // userService.getUserProfile returns user with password nulled

        mockMvc.perform(get("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.nickname").value(user.getNickname()))
                .andExpect(jsonPath("$.email").value(user.getEmail()));
    }
    
    @Test
    @WithMockUser(username = "test@example.com")
    void getCurrentUserProfile_userNotFoundInService_throwsExceptionInternal() throws Exception {
        // This tests if the principal (from @WithMockUser) is not found by findByEmail
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // Or whatever UsernameNotFoundException maps to
                // By default Spring Boot might turn it into a 500 if not handled by @ControllerAdvice
                // For this test, let's assume it becomes a 404 or similar due to UsernameNotFoundException
                // If it's 500, then an @ControllerAdvice would be needed for proper status mapping.
                // Let's check the actual controller logic: it throws UsernameNotFoundException.
                // Spring's default handler for UsernameNotFoundException (if not caught by a global handler)
                // might result in a 500. For a more specific test, a custom exception handler mapping
                // UsernameNotFoundException to 404 would be good.
                // For now, we'll expect a general server error or a specific one if defined.
                // A simple way is to ensure the controller re-throws or handles it.
                // The controller uses .orElseThrow for findByEmail.
    }


    @Test
    @WithMockUser(username = "test@example.com")
    void updateUserProfile_success() throws Exception {
        User updatedUser = new User("updatedNickname", user.getEmail(), null, "newpic.jpg");
        updatedUser.setId(user.getId());

        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userService.updateUserProfile(eq(user.getId()), anyString(), anyString())).thenReturn(updatedUser);

        UserProfileDto updateRequest = new UserProfileDto();
        updateRequest.setNickname("updatedNickname");
        updateRequest.setProfilePicture("newpic.jpg");

        mockMvc.perform(put("/api/users/me")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("updatedNickname"))
                .andExpect(jsonPath("$.profilePicture").value("newpic.jpg"));
    }
    
    @Test
    @WithMockUser(username = "test@example.com")
    void updateUserProfile_serviceThrowsSecurityException_returnsForbidden() throws Exception {
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userService.updateUserProfile(eq(user.getId()), anyString(), anyString()))
            .thenThrow(new SecurityException("User not authorized"));

        UserProfileDto updateRequest = new UserProfileDto();
        updateRequest.setNickname("updatedNickname");

        mockMvc.perform(put("/api/users/me")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("User not authorized"));
    }
}
