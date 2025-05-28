package com.example.whattowatchnow.service;

import com.example.whattowatchnow.domain.User;
import com.example.whattowatchnow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("testUser", "test@example.com", "password123", "profile.jpg");
        user.setId("userId123"); // Set an ID for existing user scenarios
    }

    // Tests for registerUser
    @Test
    void registerUser_success() throws Exception {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        User newUser = new User("testUser", "test@example.com", "password123", null);
        User registeredUser = userService.registerUser(newUser);

        assertNotNull(registeredUser);
        assertEquals("encodedPassword", registeredUser.getPassword());
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(passwordEncoder, times(1)).encode("password123");
        verify(userRepository, times(1)).save(newUser);
    }

    @Test
    void registerUser_emailAlreadyExists_throwsException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        User newUser = new User("anotherUser", "test@example.com", "newPassword", null);

        Exception exception = assertThrows(Exception.class, () -> {
            userService.registerUser(newUser);
        });

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    // Tests for loadUserByUsername
    @Test
    void loadUserByUsername_userFound_returnsUserDetails() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        UserDetails userDetails = userService.loadUserByUsername("test@example.com");

        assertNotNull(userDetails);
        assertEquals("test@example.com", userDetails.getUsername());
        assertEquals(user.getPassword(), userDetails.getPassword()); // Password should be the stored one
        assertTrue(userDetails.getAuthorities().isEmpty()); // Assuming no authorities for now
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    void loadUserByUsername_userNotFound_throwsUsernameNotFoundException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userService.loadUserByUsername("nonexistent@example.com");
        });

        assertEquals("User not found with email: nonexistent@example.com", exception.getMessage());
        verify(userRepository, times(1)).findByEmail("nonexistent@example.com");
    }
    
    // Helper to mock SecurityContext
    private void mockSecurityContext(User principalUser) {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(principalUser.getEmail());
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmail(principalUser.getEmail())).thenReturn(Optional.of(principalUser));
    }


    // Tests for updateUserProfile
    @Test
    void updateUserProfile_success_updatesOwnProfile() {
        mockSecurityContext(user); // Authenticated user is 'user'

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String newNickname = "updatedNickname";
        String newProfilePic = "updated.jpg";
        User updatedUser = userService.updateUserProfile(user.getId(), newNickname, newProfilePic);

        assertNotNull(updatedUser);
        assertEquals(newNickname, updatedUser.getNickname());
        assertEquals(newProfilePic, updatedUser.getProfilePicture());
        assertNull(updatedUser.getPassword(), "Password should not be returned"); // Check password is not returned
        verify(userRepository, times(1)).findById(user.getId());
        verify(userRepository, times(1)).save(any(User.class));
    }
    
    @Test
    void updateUserProfile_onlyNickname_updatesOwnProfile() {
        mockSecurityContext(user);

        User userToUpdate = new User(user.getNickname(), user.getEmail(), user.getPassword(), user.getProfilePicture());
        userToUpdate.setId(user.getId());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(userToUpdate));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    
        String newNickname = "onlyNicknameUpdated";
        User updatedUser = userService.updateUserProfile(user.getId(), newNickname, null); // null for profile picture URL
    
        assertNotNull(updatedUser);
        assertEquals(newNickname, updatedUser.getNickname());
        assertEquals(user.getProfilePicture(), updatedUser.getProfilePicture(), "Profile picture should not change if null passed");
        verify(userRepository).save(argThat(savedUser -> 
            savedUser.getNickname().equals(newNickname) && 
            savedUser.getProfilePicture().equals(user.getProfilePicture())
        ));
    }

    @Test
    void updateUserProfile_onlyProfilePicture_updatesOwnProfile() {
        mockSecurityContext(user);
    
        User userToUpdate = new User(user.getNickname(), user.getEmail(), user.getPassword(), user.getProfilePicture());
        userToUpdate.setId(user.getId());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(userToUpdate));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    
        String newProfilePic = "onlyPicUpdated.jpg";
        User updatedUser = userService.updateUserProfile(user.getId(), null, newProfilePic); // null for nickname
    
        assertNotNull(updatedUser);
        assertEquals(user.getNickname(), updatedUser.getNickname(), "Nickname should not change if null passed");
        assertEquals(newProfilePic, updatedUser.getProfilePicture());
        verify(userRepository).save(argThat(savedUser -> 
            savedUser.getNickname().equals(user.getNickname()) &&
            savedUser.getProfilePicture().equals(newProfilePic)
        ));
    }


    @Test
    void updateUserProfile_unauthorized_triesToUpdateAnotherUserProfile() {
        User anotherUser = new User("another", "another@example.com", "pass", "pic.png");
        anotherUser.setId("anotherUserId");
        
        mockSecurityContext(user); // Authenticated user is 'user'

        // No need to mock userRepository.findById or save as it should fail before that
        
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            userService.updateUserProfile(anotherUser.getId(), "newNick", "newPic.jpg");
        });

        assertEquals("User not authorized to update this profile.", exception.getMessage());
        verify(userRepository, never()).findById(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void updateUserProfile_targetUserNotFound_throwsException() {
        mockSecurityContext(user); // Authenticated as 'user'

        when(userRepository.findById(user.getId())).thenReturn(Optional.empty()); // User to update not found

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userService.updateUserProfile(user.getId(), "newNick", "newPic.jpg");
        });
        
        assertEquals("User not found with id: " + user.getId(), exception.getMessage());
        verify(userRepository, times(1)).findById(user.getId());
        verify(userRepository, never()).save(any(User.class));
    }


    // Tests for getUserProfile
    @Test
    void getUserProfile_userFound_returnsUserWithNullPassword() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        User foundUser = userService.getUserProfile(user.getId());

        assertNotNull(foundUser);
        assertEquals(user.getId(), foundUser.getId());
        assertEquals(user.getEmail(), foundUser.getEmail());
        assertNull(foundUser.getPassword(), "Password should be null in profile response");
        verify(userRepository, times(1)).findById(user.getId());
    }

    @Test
    void getUserProfile_userNotFound_throwsUsernameNotFoundException() {
        String nonExistentId = "nonExistentId";
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userService.getUserProfile(nonExistentId);
        });

        assertEquals("User not found with id: " + nonExistentId, exception.getMessage());
        verify(userRepository, times(1)).findById(nonExistentId);
    }
}
